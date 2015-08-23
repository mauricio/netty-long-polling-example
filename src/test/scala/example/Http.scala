package example

import com.ning.http.client.{AsyncCompletionHandler, AsyncHttpClient, Response}

import scala.concurrent.{Future, Promise}

class ErrorResponseException(val response: Response)
  extends IllegalStateException(s"HTTP request failed with ${response.getStatusCode} - ${response.getHeaders} - ${response.getResponseBody}")

object Http {

  private val log = Log.getByName(this.getClass.getCanonicalName)
  private val client = new AsyncHttpClient()

  private def perform(
                       builder: AsyncHttpClient#BoundRequestBuilder,
                       body: String,
                       headers: Map[String, String] = Map.empty): Future[Response] = {

    headers.foreach {
      case (key, value) =>
        builder.addHeader(key, value)
    }

    builder.setBody(body)

    val promise = Promise[Response]()

    builder.execute(new AsyncCompletionHandler[Response]() {

      override def onCompleted(response: Response): Response = {
        log.info(s"Request finished with ${response.getStatusCode}")

        if (response.getStatusCode() > 399) {
          log.info(s"response was ${response.getStatusCode}\n${response.getResponseBody}")
          promise.failure(new ErrorResponseException(response))
        } else {
          promise.success(response)
        }

        response
      }

      override def onThrowable(t: Throwable): Unit =
        promise.failure(t)

    })

    promise.future

  }

  def post(url: String, body: String, headers: Map[String, String] = Map.empty): Future[Response] =
    perform(client.preparePost(url), body, headers)

  def get(url: String, headers: Map[String, String] = Map.empty): Future[Response] =
    perform(client.prepareGet(url), "", headers)

}