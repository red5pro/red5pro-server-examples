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