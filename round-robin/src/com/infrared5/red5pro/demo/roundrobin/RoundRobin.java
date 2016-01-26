package com.infrared5.red5pro.demo.roundrobin;

import java.util.List;

import org.red5.server.adapter.MultiThreadedApplicationAdapter;
import org.red5.server.api.scope.IScope;

import com.red5pro.cluster.ClusterConfiguration;
import com.red5pro.cluster.plugin.ClusterLoadBalancer;
import com.red5pro.cluster.plugin.ClusterNode;
import com.red5pro.cluster.plugin.ClusterPlugin;
import com.red5pro.cluster.plugin.agent.ClusterConnection;
import com.red5pro.plugin.Red5ProPlugin;
/**
 * 
 * @author Andy Shaules
 * This application adapter replaces the default round robin with a custom implementation.
 * It creates a random-robin. 
 *
 */
public class RoundRobin extends MultiThreadedApplicationAdapter implements ClusterLoadBalancer{
	public boolean appStart(IScope scope){
		// We implement ClusterLoadBalancer and set this as the provider.
		ClusterNode.setBalancer(this);
		
		return true;
	}
	/**
	 * Round robin override.
	 */
	@Override
	public String getHost() {
		//info about the cluster can be found in the cluster config.
		ClusterConfiguration config=Red5ProPlugin.getCluster().getConfiguration();
		
		// the cluster edges can be iterated here.
		List<ClusterConnection> conns = ClusterNode.getChildren();
		//lets do a random choice.
		int r= (int) Math.round((Math.random() * conns.size()-1));
		if(conns.size()>0){
			return conns.get(r).getPublicIp()+":"+conns.get(r).getPublicPort();
		}
		
		return "0.0.0.0:0";
	}
}


