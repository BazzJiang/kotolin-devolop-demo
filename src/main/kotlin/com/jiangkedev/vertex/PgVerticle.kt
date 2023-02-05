package com.jiangkedev.vertex

import com.jiangkedev.entity.AttachmentInfoEntity
import io.smallrye.mutiny.Uni
import io.smallrye.mutiny.vertx.core.AbstractVerticle
import io.vertx.mutiny.core.http.HttpServer
import io.vertx.mutiny.ext.web.Router
import io.vertx.mutiny.ext.web.RoutingContext
import io.vertx.mutiny.ext.web.handler.BodyHandler
import org.hibernate.reactive.mutiny.Mutiny
import javax.persistence.Persistence


/**
 *@author 姜科 <bazzjiang@hotmail.com>
 *@date 2023-02-05
 *@description
 */

class PgVerticle : AbstractVerticle() {

  private var emf: Mutiny.SessionFactory? = null

  override fun asyncStart(): Uni<Void> {
    var startHibernate = Uni.createFrom().deferred {
      val props: Map<String,String> = mapOf("javax.persistence.jdbc.url" to "jdbc:postgresql://100.69.193.16:7666/crawler") // <1>
      emf = Persistence
        .createEntityManagerFactory("pg-demo", props)
        .unwrap(Mutiny.SessionFactory::class.java)
      Uni.createFrom().voidItem()
    }
    startHibernate = vertx.executeBlocking(startHibernate)
      .onItem().invoke { _ ->println("Hibernate Reactive is ready") }
    //设置路由
    val router: Router = Router.router(vertx)
    val bodyHandler: BodyHandler = BodyHandler.create()
    router.post().handler(bodyHandler::handle)
    //设置回调函数
    router.get("/attachmentList").respond(this::listAttachmentInfo);
    val startHttpServer: Uni<HttpServer> = vertx.createHttpServer()
      .requestHandler(router::handle)
      .listen(8080)
      .onItem().invoke { _->println("HTTP server listening on port 8080") }
    return Uni.combine().all().unis<Any>(startHibernate, startHttpServer).discardItems()
  }

  private fun listAttachmentInfo(ctx: RoutingContext): Uni<List<AttachmentInfoEntity>>? {
    return emf!!.withSession<List<AttachmentInfoEntity>> { session: Mutiny.Session ->
      session
        .createQuery<AttachmentInfoEntity>("from AttachmentInfoEntity", AttachmentInfoEntity::class.java)
        .getResultList()
    }
  }
}