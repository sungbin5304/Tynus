package com.sungbin.fake.nusty.tynus.dto

class ChatItem {
    var isMyChat: Boolean? = null
    var content: String? = null

    constructor() {}
    constructor(isMyChat: Boolean?, content: String?) {
        this.isMyChat = isMyChat
        this.content = content
    }

}