/*
 * Copyright 2011 Rob Fletcher
 *
 * Licensed under the Apache License, Version 2.0 (the 'License');
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an 'AS IS' BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package co.freeside.betamax.util.server

import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.handler.AbstractHandler

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import java.util.logging.Logger
import java.util.zip.DeflaterOutputStream
import java.util.zip.GZIPOutputStream

import static java.net.HttpURLConnection.HTTP_OK
import static org.eclipse.jetty.http.HttpHeaders.ACCEPT_ENCODING
import static org.eclipse.jetty.http.HttpHeaders.CONTENT_ENCODING

class EchoHandler extends AbstractHandler {

	private static final log = Logger.getLogger(EchoHandler.name)

	@Override
	void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) {
		log.fine "received $request.method request for $target"
		response.status = HTTP_OK
		response.contentType = 'text/plain'

		getResponseWriter(request, response).withWriter { writer ->
			writer << request.method << ' ' << request.requestURI
			if (request.queryString) {
				writer << '?' << request.queryString
			}
			writer << ' ' << request.protocol << '\n'
			for (headerName in request.headerNames) {
				for (header in request.getHeaders(headerName)) {
					writer << headerName << ': ' << header << '\n'
				}
			}
			request.reader.withReader { reader ->
				while (reader.ready()) {
					writer << (char) reader.read()
				}
			}
		}
	}

	private Writer getResponseWriter(HttpServletRequest request, HttpServletResponse response) {
		def out
		def acceptedEncodings = request.getHeader(ACCEPT_ENCODING)?.tokenize(',')
		log.fine "request accepts $acceptedEncodings"
		if ('gzip' in acceptedEncodings) {
			log.fine 'gzipping...'
			response.addHeader(CONTENT_ENCODING, 'gzip')
			out = new OutputStreamWriter(new GZIPOutputStream(response.outputStream))
		} else if ('deflate' in acceptedEncodings) {
			log.fine 'deflating...'
			response.addHeader(CONTENT_ENCODING, 'deflate')
			out = new OutputStreamWriter(new DeflaterOutputStream(response.outputStream))
		} else {
			log.fine 'not encoding...'
			response.addHeader(CONTENT_ENCODING, 'none')
			out = response.writer
		}
		out
	}

}
