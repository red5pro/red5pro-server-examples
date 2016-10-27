package demo.red5pro.parameters.security;
//demo.red5pro.parameters.security.ParameterDemo
import org.red5.server.adapter.MultiThreadedApplicationAdapter;
import org.red5.server.api.IConnection;
import org.red5.server.api.Red5;
import org.red5.server.api.scope.IScope;
import org.red5.server.api.stream.IStreamPlaybackSecurity;
import org.red5.server.api.stream.IStreamPublishSecurity;


public class ParameterDemo extends MultiThreadedApplicationAdapter implements IStreamPlaybackSecurity, IStreamPublishSecurity {
	
	private String secret = "some_secret_value";
	
	public boolean appStart(IScope scope){

		super.appStart(scope);		

		//To implement security handlers to prevent access to streams or publishing,
		//register the IStreamPlaybackSecurity and IStreamPublishSecurity service handler.
		//The webapp implements both interfaces.
		this.registerStreamPlaybackSecurity(this);
		this.registerStreamPublishSecurity(this);

		return true;
	}
	public boolean appConnect(IConnection conn, Object[] params){
		
		//The client params are received here. Save params of connection for later.
		conn.setAttribute("params",params);
		//log them out for fun.
		for(Object o:params){
			log.info("param {}",String.valueOf(o)); 
		}
		
		//Check the parameters and return false to reject the client.
		return validateParameters(params);
	}
	private boolean validateParameters(Object[] params) {
		
		if(params.length>0){
			if(secret.equals(params[0].toString())){
				return true;
			}
		}
		
		return false;
	}
	/**
	 * Look up or validate user param return false if not. Optionally close the client.
	 */
	@Override
	public boolean isPublishAllowed(IScope scp, String arg1, String arg2) {
		
		Object[] params = (Object[]) Red5.getConnectionLocal().getAttribute("params");
		//Check params and return false if disallowed.
		boolean badClient=false;
		for(Object o:params){
			log.info("param {}",String.valueOf(o)); 
		}
		
		if(badClient)
			Red5.getConnectionLocal().close();
		return true;
	}
	/**
	 * Look up or validate user param. return false if not. Optionally close the client.
	 */
	@Override
	public boolean isPlaybackAllowed(IScope scp, String arg1, int arg2,int arg3, boolean arg4) {
		Object[] params = (Object[]) Red5.getConnectionLocal().getAttribute("params");
		//Check params and return false if disallowed.
		boolean badClient=false;
		for(Object o:params){
			log.info("param {}",String.valueOf(o)); 
		}
		
		if(badClient)
			Red5.getConnectionLocal().close();
		return true;
	}
	public String getSecret() {
		return secret;
	}
	public void setSecret(String secret) {
		this.secret = secret;
	}
}
