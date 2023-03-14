package com.applaudo.akkalms.databseDAO

import cats.effect.IO
import com.typesafe.config.{Config, ConfigFactory}
import doobie.Transactor

object PostgresDAO {
  val config: Config = ConfigFactory.load().getConfig("postgresql")

  val url: String = config.getString("url")
  val username: String = config.getString("username")
  val password: String = config.getString("password")

 def xa = Transactor.fromDriverManager[IO](
    "org.postgresql.Driver", url, username, password
  )
}
