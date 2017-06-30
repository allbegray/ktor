package ktor

import freemarker.template.TemplateNotFoundException
import ktor.routers.board
import ktor.routers.login
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.LongIdTable
import org.jetbrains.exposed.sql.CurrentDateTime
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.ktor.application.call
import org.jetbrains.ktor.application.install
import org.jetbrains.ktor.auth.UserHashedTableAuth
import org.jetbrains.ktor.auth.authentication
import org.jetbrains.ktor.auth.basicAuthentication
import org.jetbrains.ktor.content.TextContent
import org.jetbrains.ktor.content.files
import org.jetbrains.ktor.content.static
import org.jetbrains.ktor.features.*
import org.jetbrains.ktor.freemarker.FreeMarker
import org.jetbrains.ktor.freemarker.FreeMarkerContent
import org.jetbrains.ktor.host.embeddedServer
import org.jetbrains.ktor.http.HttpStatusCode
import org.jetbrains.ktor.jetty.Jetty
import org.jetbrains.ktor.locations.Locations
import org.jetbrains.ktor.locations.location
import org.jetbrains.ktor.logging.CallLogging
import org.jetbrains.ktor.routing.get
import org.jetbrains.ktor.routing.route
import org.jetbrains.ktor.routing.routing
import org.jetbrains.ktor.util.decodeBase64

/*
location 의 data class 의 프로퍼티는 반드시 초기화를 해야 한다.

예)
@location("/board/detail")
data class BoardDetailForm(val id: Long = 0L)

위를 아래 처럼 하면 무조건 404 에러로 빠진다.

data class BoardDetailForm(val id: Long) 또는 data class BoardDetailForm(val id: Long?)

 */

@location("/login")
data class LoginForm(val loginId: String = "", val loginPassword: String = "")

@location("/board/list")
data class BoardListForm(val page: Int = 1)

@location("/board/create")
data class BoardCreateForm(val title: String = "", val content: String = "")

@location("/board/detail")
data class BoardDetailForm(val id: Long = 0L)

@location("/board/edit")
data class BoardEditForm(val id: Long = 0L, val title: String = "", val content: String = "")

@location("/board/delete")
data class BoardDeleteForm(val id: Long = 0L)

class NotFound(override val message: String? = null) : RuntimeException(message)
class BadRequest(override val message: String? = null) : RuntimeException(message)

object Boards : LongIdTable() {
    val title = varchar("title", 200)
    val content = text("content")
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime())
    val updatedAt = datetime("updated_at").nullable()
    val deletedAt = datetime("deleted_at").nullable()
}

class Board(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<Board>(Boards)

    var title by Boards.title
    var content by Boards.content
    var createdAt by Boards.createdAt
    var updatedAt by Boards.updatedAt
//    var deletedAt by Boards.deletedAt
}

object Application {
    @JvmStatic fun main(args: Array<String>) {
        Database.connect(driver = "org.postgresql.Driver", url = "jdbc:postgresql://192.168.0.100:5432/hong", user = "hong", password = "1234")
        transaction {
            logger.addLogger(StdOutSqlLogger)
            SchemaUtils.create(Boards)
            commit()
        }

        val staticFolder = File(Application.javaClass.getResource("/static").file)
        require(staticFolder.exists()) { "Cannot find ${staticFolder.absolutePath}" }

        val hashedUserTable = UserHashedTableAuth(table = mapOf(
                "test" to decodeBase64("VltM4nfheqcJSyH887H+4NEOm2tDuKCl83p5axYXlF0=") // test / test
        ))

        val server = embeddedServer(Jetty, 8080) {
            install(DefaultHeaders)
            install(Compression)
            install(CallLogging)
            install(ConditionalHeaders)
            install(PartialContentSupport)
            install(Locations)
            install(HeadRequestSupport)
            install(FreeMarker) {
                setClassForTemplateLoading(this.javaClass, "/templates")
            }
            install(StatusPages) {
                exception<NotFound> {
                    environment.log.error(it)
                    call.response.status(HttpStatusCode.NotFound)
                    call.respond(TextContent("요청한 키의 리소스를 찾을 수 없어욘..."))
                }
                exception<BadRequest> {
                    environment.log.error(it)
                    call.response.status(HttpStatusCode.BadRequest)
                    call.respond(TextContent("요청한 파라미터가 거지 같아욘..."))
                }
                exception<Throwable> {
                    environment.log.error(it)
                    call.response.status(HttpStatusCode.InternalServerError)
                    call.respond(TextContent("서버 에러 났어욘..."))
                }
                exception<TemplateNotFoundException> {
                    environment.log.error(it)
                    call.response.status(HttpStatusCode.InternalServerError)
                    call.respond(TextContent("템플릿 파일이 없습니당..."))
                }
                status(HttpStatusCode.BadRequest) {
                    call.respond(TextContent("요청 파라미터 확인하세욘..."))
                }
                status(HttpStatusCode.NotFound) {
                    call.respond(TextContent("리소스를 찾을 수 없어욘..."))
                }
                status(HttpStatusCode.Unauthorized) {
                    call.respond(TextContent("인증 정보가 없어욘..."))
                }
                status(HttpStatusCode.InternalServerError) {
                    call.respond(TextContent("서버 에러 난거임..."))
                }
            }
            routing {
                static {
                    route("/static") {
                        files(staticFolder)
                    }
                }
                login()
                board()
                route("/admin") {
                    authentication {
                        println("인증...")
                        basicAuthentication("memory-auth") {
                            hashedUserTable.authenticate(it)
                        }
                    }
                    get("/main") {
                        call.respond("관리자 메인")
                    }
                }
                get("/") {
                    val model = mapOf<String, Any>()
                    call.respond(FreeMarkerContent("index.ftlh", model))
                }
                get("/throw") {
                    throw RuntimeException("하하하")
                }
            }
        }
        server.start(wait = true)
    }
}