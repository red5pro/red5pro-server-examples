package com.infrared5.red5pro.studio.switching;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.red5.server.api.IAttributeStore;
import org.red5.server.api.IClient;
import org.red5.server.api.event.IEvent;
import org.red5.server.api.listeners.IConnectionListener;
import org.red5.server.api.scope.IBasicScope;
import org.red5.server.api.scope.IScope;
import org.red5.server.api.stream.IClientBroadcastStream;
import org.red5.server.api.stream.IClientStream;
import org.red5.server.api.stream.IPlaylistSubscriberStream;
import org.red5.server.api.stream.ISingleItemSubscriberStream;
import org.red5.server.api.stream.IStreamCapableConnection;

public class EmptyClient implements IStreamCapableConnection {

	private IScope scope;

	public EmptyClient(IScope scope) {
		this.scope=scope;
	}

	@Override
	public void addListener(IConnectionListener arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void close() {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean connect(IScope arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean connect(IScope arg0, Object[] arg1) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Iterator<IBasicScope> getBasicScopes() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IClient getClient() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getClientBytesRead() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Map<String, Object> getConnectParams() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getDroppedMessages() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Encoding getEncoding() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getHost() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getLastPingTime() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getPath() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getPendingMessages() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getProtocol() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getReadBytes() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long getReadMessages() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getRemoteAddress() {
		// TODO Auto-generated method stub
		return "localhost";
	}

	@Override
	public List<String> getRemoteAddresses() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getRemotePort() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public IScope getScope() {
		// TODO Auto-generated method stub
		return scope;
	}

	@Override
	public String getSessionId() {
		// TODO Auto-generated method stub
		return "deadbeef";
	}

	@Override
	public Number getStreamId() {
		// TODO Auto-generated method stub
		return 1;
	}

	@Override
	public String getType() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getWrittenBytes() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long getWrittenMessages() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void initialize(IClient arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean isConnected() {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public void ping() {
		// TODO Auto-generated method stub

	}

	@Override
	public void removeListener(IConnectionListener arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setBandwidth(int arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setClient(IClient arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setStreamId(Number arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void dispatchEvent(IEvent arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean handleEvent(IEvent arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void notifyEvent(IEvent arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public Boolean getBoolAttribute(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Byte getByteAttribute(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Double getDoubleAttribute(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Integer getIntAttribute(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<?> getListAttribute(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Long getLongAttribute(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<?, ?> getMapAttribute(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<?> getSetAttribute(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Short getShortAttribute(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getStringAttribute(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object getAttribute(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object getAttribute(String arg0, Object arg1) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<String> getAttributeNames() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, Object> getAttributes() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean hasAttribute(String arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean removeAttribute(String arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void removeAttributes() {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean setAttribute(String arg0, Object arg1) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean setAttributes(Map<String, Object> arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean setAttributes(IAttributeStore arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public int size() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void deleteStreamById(Number arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public long getPendingVideoMessages(Number arg0) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public IClientStream getStreamById(Number arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<Number, IClientStream> getStreamsMap() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IClientBroadcastStream newBroadcastStream(Number arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IPlaylistSubscriberStream newPlaylistSubscriberStream(Number arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ISingleItemSubscriberStream newSingleItemSubscriberStream(Number arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Number reserveStreamId() throws IndexOutOfBoundsException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Number reserveStreamId(Number arg0) throws IndexOutOfBoundsException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void unreserveStreamId(Number arg0) {
		// TODO Auto-generated method stub

	}

}
