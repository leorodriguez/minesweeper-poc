package modules

import java.time.Clock

import com.google.inject.AbstractModule


class MineSweeperModule extends AbstractModule {

  override def configure() = {
    bind(classOf[Clock]).toInstance(Clock.systemDefaultZone)

    bind(classOf[ApplicationStart]).asEagerSingleton()
  }

}
