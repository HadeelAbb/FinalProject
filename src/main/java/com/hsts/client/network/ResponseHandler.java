package com.hsts.client.network;

import com.hsts.shared.net.Response;

/**
 * Implemented by any ClientController that wants to receive Responses
 * for a given Command. The ServerConnection looks up the right handler
 * by Command and calls handleResponse on it.
 */
public interface ResponseHandler {
    void handleResponse(Response response);
}
