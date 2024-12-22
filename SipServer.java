package org.mobicents.servlet.sip.example;

import javax.servlet.sip.*;
import java.io.IOException;
import java.util.*;

public class SIPServlet extends SipServlet {

	public class Contact {
        private String contact;
        private boolean isOnline;
        
        public Contact(String contact){
			this.contact = contact;
            this.isOnline = true;
		}

		public void activate() {
			this.isOnline = true;
		}

		public void deactivate(){
            this.isOnline = true;
		}

		public boolean isActivated(){
			return this.isOnline;
		}

		public String getContact() {
			return contact;
		}

		@Override
		public String toString() {
			return "Contact [destinations=" + destinations + ", contact=" + contact + "]";
		}
	}

    private static final String DOMAIN = "acme.pt";
    private static final String PIN = "0000";
    private final String DOMAIN = "acme.pt";
    private final String BUSY_ANNOUNCE = "sip:busyann@127.0.0.1:5080";
    private final String CONF_ANNOUNCE = "sip:inconference@127.0.0.1:5080";
    private final String CONF_ROOM = "sip:conferencia@127.0.0.1:5090";
    
    private Map<String, Contact> registeredUsers = new HashMap<>();
    private Map<String, Boolean> userInConference = new HashMap<>();

    @Override
    protected void doRegister(SipServletRequest request) throws ServletException, IOException {
        String aor = getSipUri(request.getHeader("To"));
        String contact = getSipUriWithPort(request.getHeader("Contact"));

        if (!contact.endsWith("@" + DOMAIN)) {
            sendForbidden(request);
            return;
        }

        registeredUsers.put(aor, new Contact(contact));
        userInConference.put(aor, false);

        SipServletResponse response = request.createResponse(200);
        response.send();
    }

    @Override
    protected void doMessage(SipServletRequest request) throws ServletException, IOException {
        String contact = getSipUriWithPort(request.getHeader("Contact"));

        if (!contact.endsWith("@" + DOMAIN)) {
            sendForbidden(request);
            return;
        }

        String content = request.getContent().toString();
        if (PIN.equals(content)) {
            SipServletResponse response = request.createResponse(200);
            response.send();
            System.out.println("PIN verified for user: " + fromURI);
        } else {
            sendUnauthorized(request);
        }
    }

    @Override
    protected void doInvite(SipServletRequest request) throws ServletException, IOException {
        String aor = getSipUri(request.getHeader("To"));
        String contact = getSipUriWithPort(request.getHeader("Contact"));

        if (!aor.endsWith("@" + DOMAIN) || !contact.endsWith("@" + DOMAIN)) {
            sendForbidden(request);
            return;
        }

        if (!registeredUsers.containsKey(aor)) {
            sendNotFound(request);
            return;
        }

        if (userInConference.get(aor)) {
            redirectTo(request, CONF_ANNOUNCE);
            return;
        }

        SipFactory sipFactory = (SipFactory) getServletContext().getAttribute(SIP_FACTORY);
        SipServletRequest newRequest = sipFactory.createRequest(request.getApplicationSession(), request.getMethod(), request.getFrom(), request.getTo());
        newRequest.send();
        System.out.println("Call forwarded from " + fromURI + " to " + toURI);
    }

    @Override
    protected void doBye(SipServletRequest request) throws ServletException, IOException {
        String aor = getSipUri(request.getHeader("To"));

        if (!aor.endsWith("@" + DOMAIN)) {
            sendForbidden(request);
            return;
        }

        if(userInConference.containsKey(aor)){
            userInConference.set(aor, false);
        }
        
        SipServletResponse response = request.createResponse(200);
        response.send();
    }

    @Override
    protected void doInfo(SipServletRequest request) throws ServletException, IOException {
        String aor = getSipUri(request.getHeader("To"));

        if (!fromURI.endsWith("@" + DOMAIN)) {
            sendForbidden(request);
            return;
        }

        String content = request.getContent().toString();
        if ("0".equals(content)) {
            redirectTo(request, CONF_ROOM);
        }
    }

    private void redirectTo(SipServletRequest request, String destinationUri) throws ServletException, IOException {
        SipFactory sipFactory = (SipFactory) getServletContext().getAttribute(SIP_FACTORY);
        URI uri = sipFactory.createURI(destinationUri);
        request.getProxy().proxyTo(uri);
    }

    private void sendForbidden(SipServletRequest request) throws IOException {
        SipServletResponse response = request.createResponse(403);
        response.send();
    }

    private void sendUnauthorized(SipServletRequest request) throws IOException {
        SipServletResponse response = request.createResponse(401);
        response.send();
    }

    private void sendNotFound(SipServletRequest request) throws IOException {
        SipServletResponse response = request.createResponse(404);
        response.send();
    }

	protected String getSipUri(String uri) {
		String f = uri.substring(uri.indexOf("<")+1, uri.indexOf(">"));
		int indexCollon = f.indexOf(":", f.indexOf("@"));
		if (indexCollon != -1) {
			f = f.substring(0,indexCollon);
		}
		return f;
	}

	/**
	 * Auxiliary function for extracting SPI URIs
	 * @param  uri A URI with optional extra attributes
	 * @return SIP URI and port
	 */
	protected String getSipUriWithPort(String uri) {
		return uri.substring(uri.indexOf("<")+1, uri.indexOf(">"));
	}

}
