package com.github.fsanaulla.chronicler.core.model

/** Mixing mapper to context*/
trait Mappable[M[_], R] {

  /**
    * Map execution result to InfluxResult
    * @param resp - execution response, for ex: Future[HttpResponse]
    * @param f    - function that map response to specified result type
    * @tparam B   - ressult type
    * @return     - result wrapped in input container
    */
  def mapTo[B](resp: M[R], f: R => M[B]): M[B]
}
