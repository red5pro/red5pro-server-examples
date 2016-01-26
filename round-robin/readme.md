Replace Round Robin
===
This demo app adapter implements ClusterLoadBalancer interface and has a random robin set in place instead of round robin.


````java
ClusterLoadBalancer


    /**
	 * Round robin override.
	 */
	@Override
	public String getHost() {
		
		// the cluster edges can be iterated here.
		List<ClusterConnection> conns = ClusterNode.getChildren();
		//lets do a random choice.
		int r= (int) Math.round((Math.random() * conns.size()-1));
		if(conns.size()>0){
			return conns.get(r).getPublicIp()+":"+conns.get(r).getPublicPort();
		}
		
		return "0.0.0.0:0";
	}

````

Red5 Pro contains a servlet to call to get the ip. Make sure the servlet is defined in your web.xml file.
This demo app has it defined.

````java
        <servlet>

            <servlet-name>cluster</servlet-name>

            <servlet-class>

                com.red5pro.cluster.plugin.agent.ClusterWebService

            </servlet-class>

            <load-on-startup>2</load-on-startup>

        </servlet>

     

        <servlet-mapping>

            <servlet-name>cluster</servlet-name>

            <url-pattern>/cluster</url-pattern>

        </servlet-mapping>
````
You will need to link to the red5 pro jar and the red5 clustering jar.
