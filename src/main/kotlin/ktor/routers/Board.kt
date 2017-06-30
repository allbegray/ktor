package ktor.routers

import ktor.*
import org.jetbrains.exposed.sql.CurrentDateTime
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.jetbrains.ktor.application.call
import org.jetbrains.ktor.freemarker.FreeMarkerContent
import org.jetbrains.ktor.locations.get
import org.jetbrains.ktor.locations.post
import org.jetbrains.ktor.response.respondRedirect
import org.jetbrains.ktor.routing.Route

fun Route.board() {
    get<BoardListForm> {
        val boards = transaction {
            Boards
                    .slice(Boards.columns)
                    .select { Boards.deletedAt.isNull() }
                    .limit(10, (it.page - 1) * 10)
                    .map {
                        val board = Board(it[Boards.id])
                        board.title = it[Boards.title]
                        board.content = it[Boards.content]
                        board.createdAt = it[Boards.createdAt]
                        board
                    }
                    .toList()
        }

        call.respond(FreeMarkerContent("board/list.ftlh", mapOf(boards to "boards")))
    }
    get<BoardCreateForm> {
        call.respond(FreeMarkerContent("board/create.ftlh", mapOf(it to "boardCreateForm")))
    }
    post<BoardCreateForm> {
        val newId = transaction {
            val board = Board.new {
                title = it.title
                content = it.content
            }
            commit()
            board.id
        }

        call.respondRedirect("/board/detail?id=$newId")
    }
    get<BoardEditForm> {
        if (it.id == 0L) {
            throw BadRequest()
        }

        val row = transaction {
            Boards
                    .slice(Boards.fields)
                    .select { Boards.id.eq(it.id) and Boards.deletedAt.isNull() }
                    .firstOrNull()
        } ?: throw NotFound()

        val form = BoardEditForm(row[Boards.id].value, row[Boards.title], row[Boards.content])
        call.respond(FreeMarkerContent("board/edit.ftlh", mapOf(form to "boardEditForm")))
    }
    post<BoardEditForm> {
        if (it.id == 0L) {
            throw BadRequest()
        }

        val form = it
        transaction {
            Boards.update({ Boards.id eq form.id }) {
                it[title] = form.title
                it[content] = form.content
                it.update(Boards.updatedAt, CurrentDateTime())
            }
            commit()
        }

        call.respondRedirect("/board/detail?id=${form.id}")
    }
    get<BoardDetailForm> {
        if (it.id == 0L) {
            throw BadRequest()
        }

        val row = transaction {
            Boards
                    .slice(Boards.fields)
                    .select { Boards.id.eq(it.id) and Boards.deletedAt.isNull() }
                    .firstOrNull()
        } ?: throw NotFound()

        val board = Board(row[Boards.id])
        board.title = row[Boards.title]
        board.content = row[Boards.content]
        board.createdAt = row[Boards.createdAt]
        board.updatedAt = row[Boards.updatedAt]

        call.respond(FreeMarkerContent("board/detail.ftlh", mapOf(board to "board")))
    }
    get<BoardDeleteForm> {
        if (it.id == 0L) {
            throw BadRequest()
        }

        transaction {
            Boards.update({ Boards.id eq it.id }) {
                it.update(Boards.deletedAt, CurrentDateTime())
            }
            commit()
        }

        call.respondRedirect("/board")
    }
}