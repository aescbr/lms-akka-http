package com.applaudo.akkalms.actors

import akka.testkit.TestActorRef

class AuthorizationActorTest extends BaseTest {

  var authorizationActor : TestActorRef[AuthorizationActor]= null

  override def beforeAll(): Unit = {
    authorizationActor = TestActorRef[AuthorizationActor]("authorization-actor")
  }

  "authorization actor" should {
    "validate token when invalid token received" in {
      val invalidToken = "<invalid-token>"
      assert(!authorizationActor.underlyingActor.validateToken(invalidToken))
    }

    "validate token when valid token received" in {
      val validToken = "valid-token"
      authorizationActor.underlyingActor.validToken = validToken
      assert(authorizationActor.underlyingActor.validateToken(validToken))
    }
  }
}

