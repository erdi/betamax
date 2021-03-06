package co.freeside.betamax.proxy.jetty

import co.freeside.betamax.message.Request
import co.freeside.betamax.message.Response
import co.freeside.betamax.message.servlet.ServletRequestAdapter
import co.freeside.betamax.proxy.handler.HttpHandler
import co.freeside.betamax.proxy.handler.ProxyException
import co.freeside.betamax.util.message.BasicResponse
import co.freeside.betamax.util.servlet.MockHttpServletResponse
import spock.lang.Specification

import javax.servlet.http.HttpServletRequest

import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR
import static org.apache.http.HttpHeaders.ETAG
import static org.apache.http.HttpHeaders.VIA

class BetamaxProxySpec extends Specification {

	BetamaxProxy proxy = new BetamaxProxy()

	HttpServletRequest request = [:] as HttpServletRequest
	MockHttpServletResponse response = new MockHttpServletResponse()

	Response betamaxResponse

	void setup() {
		betamaxResponse = new BasicResponse(200, 'OK')
		betamaxResponse.addHeader(ETAG, UUID.randomUUID().toString())
		betamaxResponse.addHeader(VIA, 'Proxy 1, Proxy 2')
		betamaxResponse.body = 'O HAI'.bytes
	}

	void 'passes the request to is handler chain'() {
		given:
		def handler = Mock(HttpHandler)
		proxy << handler

		when:
		proxy.handle('', null, request, response)

		then:
		1 * handler.handle(_) >> { Request wrappedRequest ->
			assert wrappedRequest instanceof ServletRequestAdapter
			assert wrappedRequest.originalRequest.is(request)
			betamaxResponse
		}
	}

	void 'populates the response with whatever comes back from the handler chain'() {
		given:
		def handler = Mock(HttpHandler)
		handler.handle(_) >> betamaxResponse
		proxy << handler

		when:
		proxy.handle('', null, request, response)

		then:
		response.status == betamaxResponse.status
		response.getHeader(ETAG) == betamaxResponse.getHeader(ETAG)
		response.getHeaders(VIA).toList().containsAll(betamaxResponse.getHeader(VIA).split(/,\s*/))
		response.body == betamaxResponse.bodyAsBinary.bytes
	}

	void 'responds with the specified error status if the handler chain throws ProxyException'() {
		given:
		def handler = Mock(HttpHandler)
		handler.handle(_) >> { throw new ProxyException(419, 'I\'m a teapot') }
		proxy << handler

		when:
		proxy.handle('', null, request, response)

		then:
		response.status == 419
	}

	void 'responds with HTTP 500 if the handler chain throws any other exception'() {
		given:
		def handler = Mock(HttpHandler)
		handler.handle(_) >> { throw new IllegalStateException() }
		proxy << handler

		when:
		proxy.handle('', null, request, response)

		then:
		response.status == HTTP_INTERNAL_ERROR
	}

}
