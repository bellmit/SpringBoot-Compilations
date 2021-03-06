package com.wjwcloud.iot.voicecontrol.aligenie.test;

import com.wjwcloud.iot.customer.commons.constant.CustomerConstant;
import com.wjwcloud.iot.model.ResultResponse;
import com.wjwcloud.iot.voicecontrol.aligenie.common.AligenieConstantKey;
import com.wjwcloud.iot.voicecontrol.aligenie.test.controller.AuthzController;
import com.wjwcloud.iot.voicecontrol.aligenie.service.IAligenieAuthService;
import com.wjwcloud.iot.voicecontrol.common.ConstantKey;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.oltu.oauth2.as.issuer.MD5Generator;
import org.apache.oltu.oauth2.as.issuer.OAuthIssuer;
import org.apache.oltu.oauth2.as.issuer.OAuthIssuerImpl;
import org.apache.oltu.oauth2.as.request.OAuthAuthzRequest;
import org.apache.oltu.oauth2.as.request.OAuthTokenRequest;
import org.apache.oltu.oauth2.as.response.OAuthASResponse;
import org.apache.oltu.oauth2.client.OAuthClient;
import org.apache.oltu.oauth2.client.URLConnectionClient;
import org.apache.oltu.oauth2.client.request.OAuthBearerClientRequest;
import org.apache.oltu.oauth2.client.request.OAuthClientRequest;
import org.apache.oltu.oauth2.client.response.OAuthAuthzResponse;
import org.apache.oltu.oauth2.client.response.OAuthJSONAccessTokenResponse;
import org.apache.oltu.oauth2.client.response.OAuthResourceResponse;
import org.apache.oltu.oauth2.common.OAuth;
import org.apache.oltu.oauth2.common.error.OAuthError;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.apache.oltu.oauth2.common.message.OAuthResponse;
import org.apache.oltu.oauth2.common.message.types.GrantType;
import org.apache.oltu.oauth2.common.message.types.ParameterStyle;
import org.apache.oltu.oauth2.rs.request.OAuthAccessResourceRequest;
import org.apache.oltu.oauth2.rs.response.OAuthRSResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/test")
public class Oauth2Test {


    private static Logger logger = LoggerFactory.getLogger(AuthzController.class);

    private Cache cache;

    @Autowired
    public Oauth2Test(CacheManager cacheManager) {
        this.cache = cacheManager.getCache("oauth2-cache");
    }

    /**
     * 注入天猫精灵登录服务
     */
    @Resource(name = "aligenieLoginServiceImpl")
    private IAligenieAuthService iAligenieAuthService;



    @RequestMapping("/client")
    public String  loginPage(HttpServletRequest request, Model model){
        try {
            System.out.println("执行到这里");
            OAuthClientRequest oauthResponse = OAuthClientRequest
                    .authorizationLocation(AligenieConstantKey.OAUTH_CLIENT_AUTHORIZE)
                    .setResponseType(OAuth.OAUTH_CODE)
                    .setClientId(AligenieConstantKey.OAUTH_CLIENT_ID)
                    .setRedirectURI(AligenieConstantKey.OAUTH_CLIENT_CALLBACK)
                    .setScope(AligenieConstantKey.OAUTH_CLIENT_SCOPE)
                    .setState("111")
                    .buildQueryMessage();
            System.out.println("flow==client:"+oauthResponse.getLocationUri());
            return "redirect:" + oauthResponse.getLocationUri();
        } catch (OAuthSystemException e) {
            e.printStackTrace();
        }
        return "index";
    }

    @RequestMapping(value = "/authorize")
    public String authorize(HttpServletRequest request, HttpSession session, Model model)
            throws OAuthSystemException, IOException {
        System.out.println("flow==auth start");
        try {

            //构建OAuth请求
            OAuthAuthzRequest oauthRequest = new OAuthAuthzRequest(request);

            //查询客户端Appkey应用的信息
            String clientName= "App Name";
            model.addAttribute("clientName",clientName);
            model.addAttribute("response_type",oauthRequest.getResponseType());
            model.addAttribute("client_id",oauthRequest.getClientId());
            model.addAttribute("redirect_uri",oauthRequest.getRedirectURI());
            model.addAttribute("state",oauthRequest.getState());
            model.addAttribute("scope",oauthRequest.getScopes());
            //验证用户是否已登录
            if(session.getAttribute(AligenieConstantKey.MEMBER_SESSION_KEY)==null) {
                //用户登录
                if("1".equals(validateOAuth2Pwd(request).getCode())) {
                    //登录失败跳转到登陆页
                    System.out.println("flow==oauth2/login");
                    return "index";
                }
            }
            //判断此次请求是否是用户授权
            if(request.getParameter("action")==null||!request.getParameter("action").equalsIgnoreCase("authorize")){
                //到申请用户同意授权页
                System.out.println("flow==oauth2/authorize");
                return "authorize";
            }
            //生成授权码 UUIDValueGenerator OR MD5Generator
            String authorizationCode = new OAuthIssuerImpl(new MD5Generator()).authorizationCode();
            //把授权码存入缓存
            cache.put(authorizationCode, DigestUtils.sha1Hex(oauthRequest.getClientId()+oauthRequest.getRedirectURI()));
            //构建oauth2授权返回信息
            OAuthResponse oauthResponse = OAuthASResponse
                    .authorizationResponse(request, HttpServletResponse.SC_FOUND)
                    .setCode(authorizationCode)
                    .location(oauthRequest.getParam(OAuth.OAUTH_REDIRECT_URI))
                    .buildQueryMessage();
            //申请令牌成功重定向到客户端页
            System.out.println("flow==auth:"+oauthResponse.getLocationUri());
            return "redirect:"+oauthResponse.getLocationUri();
        } catch(OAuthProblemException ex) {
            OAuthResponse oauthResponse = OAuthResponse
                    .errorResponse(HttpServletResponse.SC_UNAUTHORIZED)
                    .error(ex)
                    .buildJSONMessage();
            logger.error("oauthRequest.getRedirectURI() : " + ex.getRedirectUri() + " oauthResponse.getBody() : " + oauthResponse.getBody());
            model.addAttribute("errorMsg", oauthResponse.getBody());
            return  "/oauth2/error";
        }
    }

    @RequestMapping(value = "/oauthCallback" ,method = RequestMethod.GET)
    public String getToken(HttpServletRequest request,Model model) throws OAuthProblemException {
        System.out.println("flow==client callback start");
        OAuthAuthzResponse oauthAuthzResponse = null;
        try {
            oauthAuthzResponse = OAuthAuthzResponse.oauthCodeAuthzResponse(request);
            String code = oauthAuthzResponse.getCode();
            OAuthClientRequest oauthClientRequest = OAuthClientRequest
                    .tokenLocation(AligenieConstantKey.OAUTH_CLIENT_ACCESS_TOKEN)
                    .setGrantType(GrantType.AUTHORIZATION_CODE)
                    .setClientId(ConstantKey.OAUTH_CLIENT_ID)
                    .setClientSecret(ConstantKey.OAUTH_CLIENT_SECRET)
                    .setRedirectURI(AligenieConstantKey.OAUTH_CLIENT_CALLBACK)
                    .setCode(code)
                    .buildQueryMessage();
            OAuthClient oAuthClient = new OAuthClient(new URLConnectionClient());

            //Facebook is not fully compatible with OAuth 2.0 draft 10, access token response is
            //application/x-www-form-urlencoded, not json encoded so we use dedicated response class for that
            //Custom response classes are an easy way to deal with oauth providers that introduce modifications to
            //OAuth 2.0 specification

            //获取access token
            OAuthJSONAccessTokenResponse oAuthResponse = oAuthClient.accessToken(oauthClientRequest, OAuth.HttpMethod.POST);
            String accessToken = oAuthResponse.getAccessToken();
            String refreshToken= oAuthResponse.getRefreshToken();
            Long expiresIn = oAuthResponse.getExpiresIn();
            //获得资源服务
            OAuthClientRequest bearerClientRequest = new OAuthBearerClientRequest(AligenieConstantKey.OAUTH_GET_SOURCE)
                    .setAccessToken(accessToken).buildQueryMessage();
            OAuthResourceResponse resourceResponse = oAuthClient.resource(bearerClientRequest, OAuth.HttpMethod.GET, OAuthResourceResponse.class);
            String resBody = resourceResponse.getBody();

            logger.info("accessToken: "+accessToken +" refreshToken: "+refreshToken +" expiresIn: "+expiresIn +" resBody: "+resBody);
            model.addAttribute("accessToken",  "accessToken: "+accessToken +" resBody: "+resBody);
            return "token";
        } catch (OAuthSystemException ex) {
            logger.error("getToken OAuthSystemException : " + ex.getMessage());
            model.addAttribute("errorMsg",  ex.getMessage());
            return  "/oauth2/error";
        }
    }

    @RequestMapping(value = "/accessToken",method = RequestMethod.POST)
    public void accessToken(HttpServletRequest request, HttpServletResponse response)
            throws IOException, OAuthSystemException {
        logger.info("智能音箱授权成功回调");
        System.out.println("flow==access token start");
        PrintWriter out = null;
        OAuthIssuer oauthIssuerImpl = new OAuthIssuerImpl(new MD5Generator());
        try {
            out = response.getWriter();
            //构建oauth2请求
            OAuthTokenRequest oauthRequest = new OAuthTokenRequest(request);

            String authzCode = oauthRequest.getCode();
            //验证AUTHORIZATION_CODE , 其他的还有PASSWORD 或 REFRESH_TOKEN (考虑到更新令牌的问题，在做修改)
            if (GrantType.AUTHORIZATION_CODE.name().equalsIgnoreCase(oauthRequest.getParam(OAuth.OAUTH_GRANT_TYPE))) {
                if (cache.get(authzCode) == null) {
                    OAuthResponse oauthResponse = OAuthASResponse
                            .errorResponse(HttpServletResponse.SC_UNAUTHORIZED)
                            .setError(OAuthError.TokenResponse.INVALID_GRANT)
                            .setErrorDescription(AligenieConstantKey.INVALID_CLIENT_GRANT)
                            .buildJSONMessage();
                    out.write(oauthResponse.getBody());
                    out.flush();
                    out.close();
                    return;
                }
            }
            //生成token
            final String accessToken = oauthIssuerImpl.accessToken();
            String refreshToken = oauthIssuerImpl.refreshToken();
            //cache.put(accessToken,cache.get(authzCode).get());
            cache.put(refreshToken,accessToken);
            logger.info("accessToken : "+accessToken +"  refreshToken: "+refreshToken);
            //清除授权码 确保一个code只能使用一次
            cache.evict(authzCode);
            //构建oauth2授权返回信息
            OAuthResponse oauthResponse = OAuthASResponse
                    .tokenResponse(HttpServletResponse.SC_OK)
                    .setAccessToken(accessToken)
                    .setExpiresIn("3600")
                    .setRefreshToken(refreshToken)
                    .buildJSONMessage();
            response.setStatus(oauthResponse.getResponseStatus());
            response.setContentType("application/json");
            out.print(oauthResponse.getBody());
            out.flush();
            out.close();
        } catch(OAuthProblemException ex) {
            OAuthResponse oauthResponse = OAuthResponse
                    .errorResponse(HttpServletResponse.SC_UNAUTHORIZED)
                    .error(ex)
                    .buildJSONMessage();
            response.setStatus(oauthResponse.getResponseStatus());
            out.print(oauthResponse.getBody());
            out.flush();
            out.close();
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
        }
        finally
        {
            if (null != out){ out.close();}
        }
    }

    @RequestMapping("/getResource")
//    @ResponseBody
    public void get_resource(HttpServletRequest request, HttpServletResponse response)
            throws IOException, OAuthSystemException{
        PrintWriter out = null;
        try {
            System.out.println("flow==getResource Start");
            out = response.getWriter();
            //构建oauth2资源请求
            OAuthAccessResourceRequest oauthRequest = new OAuthAccessResourceRequest(request, ParameterStyle.QUERY);
            //获取验证accesstoken
            String accessToken = oauthRequest.getAccessToken();
            System.out.println("flow==accessToken="+oauthRequest.getAccessToken());
            //验证accesstoken是否存在或过期
//            if (accessToken.isEmpty()||cache.get(accessToken)== null) {
//                OAuthResponse oauthResponse = OAuthRSResponse
//                        .errorResponse(HttpServletResponse.SC_UNAUTHORIZED)
//                        .setRealm("RESOURCE_SERVER_NAME")
//                        .setError(OAuthError.ResourceResponse.INVALID_TOKEN)
//                        .setErrorDescription(OAuthError.ResourceResponse.EXPIRED_TOKEN)
//                        .buildHeaderMessage();
//                response.addDateHeader(OAuth.HeaderType.WWW_AUTHENTICATE, Long.parseLong(oauthResponse.getHeader(OAuth.HeaderType.WWW_AUTHENTICATE)));
//            }
            //获得用户名
            String mobilephone = "16620069844"; //oAuthService.getNameByAccessToken(accessToken);
            out.print(mobilephone);
            out.flush();
            out.close();
        } catch (OAuthProblemException ex) {
            logger.error("ResourceController OAuthProblemException : "+ex.getMessage());
            OAuthResponse oauthResponse = OAuthRSResponse
                    .errorResponse(HttpServletResponse.SC_UNAUTHORIZED)
                    .setRealm("get_resource exception")
                    .buildHeaderMessage();
            response.addDateHeader(OAuth.HeaderType.WWW_AUTHENTICATE, Long.parseLong(oauthResponse.getHeader(OAuth.HeaderType.WWW_AUTHENTICATE)));
        }
        finally {
            if (null != out){ out.close();}
        }
    }

    private ResultResponse validateOAuth2Pwd(HttpServletRequest request){

        String mobilePhone = request.getParameter("mobilePhone");
        String password = request.getParameter("password");
        if(StringUtils.isBlank(mobilePhone)){
            return ResultResponse.FAILED();
        }
//            if(!params.containsKey("loginType")){
//                return false;
//            }
        Map<String,Object> params = new HashMap<>();
        params.put("mobilePhone",mobilePhone);
        params.put("password",password);
        params.put("loginType", CustomerConstant.LOGIN_TYPE_PASSWORD);
        Map token = iAligenieAuthService.login(params);
        if (null != token) {
            request.getSession().setAttribute(AligenieConstantKey.MEMBER_SESSION_KEY,mobilePhone);
            return ResultResponse.SUCCESSFUL(token);
        } else {
            return ResultResponse.FAILED();
        }
    }

}
