package org.secureauth.sarestapi;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

import org.secureauth.sarestapi.data.AdaptiveAuthRequest;
import org.secureauth.sarestapi.data.AdaptiveAuthResponse;
import org.secureauth.sarestapi.data.AuthRequest;
import org.secureauth.sarestapi.data.FactorsResponse;
import org.secureauth.sarestapi.data.IPEval;
import org.secureauth.sarestapi.data.IPEvalRequest;
import org.secureauth.sarestapi.data.PushAcceptDetails;
import org.secureauth.sarestapi.data.PushAcceptStatus;
import org.secureauth.sarestapi.data.PushToAcceptRequest;
import org.secureauth.sarestapi.data.ResponseObject;
import org.secureauth.sarestapi.data.SAAuth;
import org.secureauth.sarestapi.data.SABaseURL;
import org.secureauth.sarestapi.queries.AuthQuery;
import org.secureauth.sarestapi.queries.FactorsQuery;
import org.secureauth.sarestapi.queries.IPEvalQuery;
import org.secureauth.sarestapi.resources.SAExecuter;
import org.secureauth.sarestapi.util.RestApiHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author rrowcliffe@secureauth.com
 * <p>
 *     SAAccess is a class that allows access to the SecureAuth REST API. The intention is to provide an easy method to access
 *     the Secureauth Authentication Rest Services.
 * </p>
 *
 * <p>
 * Copyright 2015 SecureAuth Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * </p>
 */

 public class SAAccess {

    private static Logger logger = LoggerFactory.getLogger(SAAccess.class);
    protected SABaseURL saBaseURL;
    protected SAAuth saAuth;
    protected SAExecuter saExecuter;

    /**
     *<p>
     *     Returns a SAAccess Object that can be used to query the SecureAuth Rest API
     *     This should be the default object used when setting up connectivity to the SecureAuth Appliance
     *</p>
     * @param host FQDN of the SecureAuth Appliance
     * @param port The port used to access the web application on the Appliance.
     * @param ssl Use SSL
     * @param realm the Configured Realm that enables the RESTApi
     * @param applicationID The Application ID from the Configured Realm
     * @param applicationKey The Application Key from the Configured Realm
     */
    public SAAccess(String host, String port,boolean ssl, String realm, String applicationID, String applicationKey){
        saBaseURL=new SABaseURL(host,port,ssl);
        saAuth = new SAAuth(applicationID,applicationKey,realm);
        saExecuter=new SAExecuter();
    }


    /**
     * <p>
     *     Returns IP Risk Evaluation from the Rest API
     * </p>
     * @param userid The User ID that you want to validate from
     * @param ip_address The IP Address of the user making the request for access
     * @return {@link org.secureauth.sarestapi.data.IPEval}
     *
     */
    public IPEval iPEvaluation(String userid, String ip_address){
        String ts = getServerTime();
        RestApiHeader restApiHeader =new RestApiHeader();
        IPEvalRequest ipEvalRequest =new IPEvalRequest();
        ipEvalRequest.setIp_address(ip_address);
        ipEvalRequest.setUser_id(userid);
        ipEvalRequest.setType("risk");

        String header = restApiHeader.getAuthorizationHeader(saAuth,"POST", IPEvalQuery.queryIPEval(saAuth.getRealm()), ipEvalRequest, ts);

        try{

            return saExecuter.executeIPEval(header,saBaseURL.getApplianceURL() + IPEvalQuery.queryIPEval(saAuth.getRealm()),ipEvalRequest,ts);

        }catch (Exception e){
            logger.error(new StringBuilder().append("Exception occurred executing REST query::\n").append(e.getMessage()).append("\n").toString(), e);
        }

        return null;
    }

    /**
     * <p>
     *     Returns the list of Factors available for the specified user
     * </p>
     * @param userid the userid of the identity you wish to have a list of possible second factors
     * @return {@link org.secureauth.sarestapi.data.FactorsResponse}
     */
    public FactorsResponse factorsByUser(String userid){
        String ts = getServerTime();
        RestApiHeader restApiHeader = new RestApiHeader();
        String header = restApiHeader.getAuthorizationHeader(saAuth,"GET",FactorsQuery.queryFactors(saAuth.getRealm(),userid),ts);


        try{
            return saExecuter.executeGetRequest(header,saBaseURL.getApplianceURL() + FactorsQuery.queryFactors(saAuth.getRealm(),userid),ts, FactorsResponse.class);

        }catch (Exception e){
        logger.error(new StringBuilder().append("Exception occurred executing REST query::\n").append(e.getMessage()).append("\n").toString(), e);
    }
        return null;
    }
    
	public String executeGetRequest(String query) {
		String ts = getServerTime();
		RestApiHeader restApiHeader = new RestApiHeader();
		query = saAuth.getRealm() + query;
		String header = restApiHeader.getAuthorizationHeader(saAuth, "GET", query, ts);
		try {
			return saExecuter.executeRawGetRequest(header, saBaseURL.getApplianceURL() + query, ts);
		} catch (Exception e) {
			logger.error("Exception occurred executing REST query::\n" + e.getMessage() + "\n", e);
		}
		return null;
	}
    
    /**
     * <p>
     *     Send push to accept request asynchronously 
     * </p>
     * @param userid the user id of the identity
     * @param endUserIP the IP of requesting client
     * @return {@link org.secureauth.sarestapi.data.FactorsResponse}
     */
    public ResponseObject sendPushToAcceptReq(String userid, String factor_id, String endUserIP, String clientCompany, String clientDescription){
        String ts = getServerTime();
        RestApiHeader restApiHeader = new RestApiHeader();
        PushToAcceptRequest req = new PushToAcceptRequest();
        req.setUser_id(userid);
        req.setType("push_accept");
        req.setFactor_id(factor_id);
        PushAcceptDetails pad = new PushAcceptDetails();
        pad.setEnduser_ip(endUserIP);
        if (clientCompany != null) {
        	pad.setCompany_name(clientCompany);
        }
        if (clientDescription != null) {
        	pad.setApplication_description(clientDescription);
        }
        req.setPush_accept_details(pad);
        String header = restApiHeader.getAuthorizationHeader(saAuth,"POST", AuthQuery.queryAuth(saAuth.getRealm()), req,ts);

        try{
            return saExecuter.executePostRequest(header,saBaseURL.getApplianceURL() + AuthQuery.queryAuth(saAuth.getRealm()), req,ts, ResponseObject.class);
        }catch (Exception e){
            logger.error(new StringBuilder().append("Exception occurred executing REST query::\n").append(e.getMessage()).append("\n").toString(), e);
        }
        return null;
    }
    
    /**
     * <p>
     *     Perform adaptive auth query
     * </p>
     * @param userid the user id of the identity
     * @param endUserIP the IP of requesting client
     * @return {@link org.secureauth.sarestapi.data.FactorsResponse}
     */
    public AdaptiveAuthResponse adaptiveAuthQuery(String userid, String endUserIP){
        String ts = getServerTime();
        RestApiHeader restApiHeader = new RestApiHeader();
        AdaptiveAuthRequest req = new AdaptiveAuthRequest(userid, endUserIP);
        String header = restApiHeader.getAuthorizationHeader(saAuth,"POST", AuthQuery.queryAAuth(saAuth.getRealm()), req,ts);

        try{
            return saExecuter.executePostRequest(header,saBaseURL.getApplianceURL() + AuthQuery.queryAAuth(saAuth.getRealm()), req, ts, AdaptiveAuthResponse.class);
        }catch (Exception e){
            logger.error(new StringBuilder().append("Exception occurred executing REST query::\n").append(e.getMessage()).append("\n").toString(), e);
        }
        return null;
    }
    
    public PushAcceptStatus queryPushAcceptStatus(String refId){
        String ts = getServerTime();
        RestApiHeader restApiHeader = new RestApiHeader();
        String getUri = AuthQuery.queryAuth(saAuth.getRealm()) + "/" + refId;
        String header = restApiHeader.getAuthorizationHeader(saAuth,"GET", getUri,ts);

        try{
            return saExecuter.executeGetRequest(header,saBaseURL.getApplianceURL() + getUri,ts, PushAcceptStatus.class);
        }catch (Exception e){
            logger.error(new StringBuilder().append("Exception occurred executing REST query::\n").append(e.getMessage()).append("\n").toString(), e);
        }
        return null;
    }
    
    public static String encode(String input) {
        StringBuilder resultStr = new StringBuilder();
        for (char ch : input.toCharArray()) {
            if (isUnsafe(ch)) {
                resultStr.append('%');
                resultStr.append(toHex(ch / 16));
                resultStr.append(toHex(ch % 16));
            } else {
                resultStr.append(ch);
            }
        }
        return resultStr.toString();
    }

    private static char toHex(int ch) {
        return (char) (ch < 10 ? '0' + ch : 'A' + ch - 10);
    }

    private static boolean isUnsafe(char ch) {
        if (ch > 128 || ch < 0)
            return true;
        return " %$&+,/:;=?@<>#%".indexOf(ch) >= 0;
    }

    /**
     *
     * <p>
     *     Checks if the Username exists within the datastore within SecureAuth
     * </p>
     * @param userid the userid of the identity
     * @return {@link org.secureauth.sarestapi.data.ResponseObject}
     */
    public ResponseObject validateUser(String userid){

        String ts = getServerTime();
        RestApiHeader restApiHeader = new RestApiHeader();
        AuthRequest authRequest = new AuthRequest();

        authRequest.setUser_id(userid);
        authRequest.setType("user_id");

        String header = restApiHeader.getAuthorizationHeader(saAuth,"POST", AuthQuery.queryAuth(saAuth.getRealm()), authRequest,ts);


        try{
            return saExecuter.executeValidateUser(header,saBaseURL.getApplianceURL() + AuthQuery.queryAuth(saAuth.getRealm()),authRequest,ts);
        }catch (Exception e){
            logger.error(new StringBuilder().append("Exception occurred executing REST query::\n").append(e.getMessage()).append("\n").toString(), e);
        }
        return null;
    }

    /**
     * <p>
     *     Checks the users password against SecureAuth Datastore
     * </p>
     * @param userid the userid of the identity
     * @param password The password of the user to validate
     * @return {@link org.secureauth.sarestapi.data.ResponseObject}
     */
    public ResponseObject validateUserPassword(String userid, String password){
        String ts = getServerTime();
        RestApiHeader restApiHeader = new RestApiHeader();
        AuthRequest authRequest = new AuthRequest();

        authRequest.setUser_id(userid);
        authRequest.setType("password");
        authRequest.setToken(password);

        String header = restApiHeader.getAuthorizationHeader(saAuth,"POST", AuthQuery.queryAuth(saAuth.getRealm()), authRequest,ts);

        try{
            return saExecuter.executeValidateUserPassword(header,saBaseURL.getApplianceURL() + AuthQuery.queryAuth(saAuth.getRealm()),authRequest,ts);
        }catch (Exception e){
            logger.error(new StringBuilder().append("Exception occurred executing REST query::\n").append(e.getMessage()).append("\n").toString(), e);
        }
        return null;
    }

    /**
     * <p>
     *     Validate the users Answer to a KB Question
     * </p>
     * @param userid the userid of the identity
     * @param answer The answer to the KBA
     * @param factor_id the KB Id to be compared against
     * @return {@link org.secureauth.sarestapi.data.ResponseObject}
     */
    public ResponseObject validateKba(String userid, String answer, String factor_id){
        String ts = getServerTime();
        RestApiHeader restApiHeader = new RestApiHeader();
        AuthRequest authRequest = new AuthRequest();

        authRequest.setUser_id(userid);
        authRequest.setType("kba");
        authRequest.setToken(answer);
        authRequest.setFactor_id(factor_id);

        String header = restApiHeader.getAuthorizationHeader(saAuth,"POST", AuthQuery.queryAuth(saAuth.getRealm()), authRequest,ts);

        try{
            return saExecuter.executeValidateKba(header,saBaseURL.getApplianceURL() + AuthQuery.queryAuth(saAuth.getRealm()),authRequest,ts);
        }catch (Exception e){
            logger.error(new StringBuilder().append("Exception occurred executing REST query::\n").append(e.getMessage()).append("\n").toString(), e);
        }
        return null;
    }

    /**
     *<p>
     *     Validate the Oath Token
     *</p>
     * @param userid the userid of the identity
     * @param otp The One Time Passcode to validate
     * @param factor_id The Device Identifier
     * @return {@link org.secureauth.sarestapi.data.ResponseObject}
     */
    public ResponseObject validateOath(String userid, String otp, String factor_id){
        String ts = getServerTime();
        RestApiHeader restApiHeader = new RestApiHeader();
        AuthRequest authRequest = new AuthRequest();

        authRequest.setUser_id(userid);
        authRequest.setType("oath");
        authRequest.setToken(otp);
        authRequest.setFactor_id(factor_id);

        String header = restApiHeader.getAuthorizationHeader(saAuth,"POST", AuthQuery.queryAuth(saAuth.getRealm()), authRequest,ts);

        try{
            return saExecuter.executeValidateOath(header,saBaseURL.getApplianceURL() + AuthQuery.queryAuth(saAuth.getRealm()),authRequest,ts);
        }catch (Exception e){
            logger.error(new StringBuilder().append("Exception occurred executing REST query::\n").append(e.getMessage()).append("\n").toString(), e);
        }
        return null;
    }

    /**
     * <p>
     *     Send One Time Passcode by Phone
     * </p>
     * @param userid the userid of the identity
     * @param factor_id  Phone Property   "Phone1"
     * @return {@link org.secureauth.sarestapi.data.ResponseObject}
     */
    public ResponseObject deliverOTPByPhone(String userid, String factor_id){
        String ts = getServerTime();
        RestApiHeader restApiHeader = new RestApiHeader();
        AuthRequest authRequest = new AuthRequest();

        authRequest.setUser_id(userid);
        authRequest.setType("call");
        authRequest.setFactor_id(factor_id);

        String header = restApiHeader.getAuthorizationHeader(saAuth,"POST", AuthQuery.queryAuth(saAuth.getRealm()), authRequest,ts);

        try{
            return saExecuter.executeOTPByPhone(header,saBaseURL.getApplianceURL() + AuthQuery.queryAuth(saAuth.getRealm()),authRequest,ts);
        }catch (Exception e){
            logger.error(new StringBuilder().append("Exception occurred executing REST query::\n").append(e.getMessage()).append("\n").toString(), e);
        }
        return null;
    }

    /**
     * <p>
     *     Send One Time Passcode by SMS
     * </p>
     * @param userid the userid of the identity
     * @param factor_id  Phone Property   "Phone1"
     * @return {@link org.secureauth.sarestapi.data.ResponseObject}
     */
    public ResponseObject deliverOTPBySMS(String userid, String factor_id){
        String ts = getServerTime();
        RestApiHeader restApiHeader = new RestApiHeader();
        AuthRequest authRequest = new AuthRequest();

        authRequest.setUser_id(userid);
        authRequest.setType("sms");
        authRequest.setFactor_id(factor_id);
        String header = restApiHeader.getAuthorizationHeader(saAuth,"POST", AuthQuery.queryAuth(saAuth.getRealm()), authRequest,ts);

        try{
            return saExecuter.executeOTPBySMS(header,saBaseURL.getApplianceURL() + AuthQuery.queryAuth(saAuth.getRealm()),authRequest,ts);
        }catch (Exception e){
            logger.error(new StringBuilder().append("Exception occurred executing REST query::\n").append(e.getMessage()).append("\n").toString(), e);
        }
        return null;
    }

    /**
     * <p>
     *     Send One Time Passcode by Email
     * </p>
     * @param userid the userid of the identity
     * @param factor_id  Email Property   "Email1"
     * @return {@link org.secureauth.sarestapi.data.ResponseObject}
     */
    public ResponseObject deliverOTPByEmail(String userid, String factor_id){
        String ts = getServerTime();
        RestApiHeader restApiHeader = new RestApiHeader();
        AuthRequest authRequest = new AuthRequest();

        authRequest.setUser_id(userid);
        authRequest.setType("email");
        authRequest.setFactor_id(factor_id);
        String header = restApiHeader.getAuthorizationHeader(saAuth,"POST", AuthQuery.queryAuth(saAuth.getRealm()), authRequest,ts);

        try{
            return saExecuter.executeOTPByEmail(header,saBaseURL.getApplianceURL() + AuthQuery.queryAuth(saAuth.getRealm()), authRequest,ts);
        }catch (Exception e){
            logger.error(new StringBuilder().append("Exception occurred executing REST query::\n").append(e.getMessage()).append("\n").toString(), e);
        }
        return null;
    }

    /**
     * <p>
     *     Send One Time Passcode by Push
     * </p>
     * @param userid the userid of the identity
     * @param factor_id  Device Property   "z0y9x87wv6u5t43srq2p1on"
     * @return {@link org.secureauth.sarestapi.data.ResponseObject}
     */
    public ResponseObject deliverOTPByPush(String userid, String factor_id){
        String ts = getServerTime();
        RestApiHeader restApiHeader = new RestApiHeader();
        AuthRequest authRequest = new AuthRequest();

        authRequest.setUser_id(userid);
        authRequest.setType("push");
        authRequest.setFactor_id(factor_id);
        String header = restApiHeader.getAuthorizationHeader(saAuth,"POST", AuthQuery.queryAuth(saAuth.getRealm()), authRequest,ts);

        try{
            return saExecuter.executePostRequest(header,saBaseURL.getApplianceURL() + AuthQuery.queryAuth(saAuth.getRealm()), authRequest,ts, ResponseObject.class);
        }catch (Exception e){
            logger.error(new StringBuilder().append("Exception occurred executing REST query::\n").append(e.getMessage()).append("\n").toString(), e);
        }
        return null;
    }

    /**
     * <p>
     *     Send One Time Passcode by Helpdesk
     * </p>
     * @param userid the userid of the identity
     * @param factor_id  Help Desk Property   "HelpDesk1"
     * @return {@link org.secureauth.sarestapi.data.ResponseObject}
     */
    public ResponseObject deliverOTPByHelpDesk(String userid, String factor_id){
        String ts = getServerTime();
        RestApiHeader restApiHeader = new RestApiHeader();
        AuthRequest authRequest = new AuthRequest();

        authRequest.setUser_id(userid);
        authRequest.setType("help_desk");
        authRequest.setFactor_id(factor_id);
        String header = restApiHeader.getAuthorizationHeader(saAuth,"POST", AuthQuery.queryAuth(saAuth.getRealm()), authRequest,ts);

        try{
            return saExecuter.executeOTPByHelpDesk(header,saBaseURL.getApplianceURL() + AuthQuery.queryAuth(saAuth.getRealm()),authRequest,ts);
        }catch (Exception e){
            logger.error(new StringBuilder().append("Exception occurred executing REST query::\n").append(e.getMessage()).append("\n").toString(), e);
        }
        return null;
    }



    String getServerTime() {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "EEE, dd MMM yyyy HH:mm:ss.SSS z", Locale.US);
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        return dateFormat.format(calendar.getTime());
    }
}
