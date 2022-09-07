package ilove.quark.us

import java.io.IOException
import jakarta.enterprise.context.ApplicationScoped
import jakarta.websocket.*
import jakarta.websocket.server.PathParam
import jakarta.websocket.server.ServerEndpoint

@ServerEndpoint("/start-websocket/{name}")
@ApplicationScoped
class StartWebSocket {

    @OnOpen
    fun onOpen(session: Session?, @PathParam("name") name: String) {
        println("onOpen> $name")
    }

    @OnClose
    fun onClose(session: Session?, @PathParam("name") name: String) {
        println("onClose> $name")
    }

    @OnError
    fun onError(session: Session?, @PathParam("name") name: String, throwable: Throwable) {
        println("onError> $name: $throwable")
    }

    @OnMessage
    fun onMessage(message: String, @PathParam("name") name: String) {
        println("onMessage> $name: $message")
    }
}
