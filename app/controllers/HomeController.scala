package controllers

import javax.inject._
import play.api.mvc._
import services.ApiService

import scala.concurrent.ExecutionContext

@Singleton
class HomeController @Inject()(cc: ControllerComponents, api: ApiService)
                              (implicit ec: ExecutionContext, assetsFinder: AssetsFinder)
  extends AbstractController(cc) {


}
