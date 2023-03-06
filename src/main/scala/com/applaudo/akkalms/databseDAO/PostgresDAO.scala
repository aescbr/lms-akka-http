package com.applaudo.akkalms.databseDAO

import cats.effect.IO
import doobie.Transactor

object PostgresDAO {
 def xa = Transactor.fromDriverManager[IO](
    "org.postgresql.Driver",
    "jdbc:postgresql://localhost:5432/latest",
    "latest",
    "latest"
  )
}
