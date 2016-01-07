package com.kaibla.hamster.example.ui;

import com.kaibla.hamster.base.HamsterPage;
import com.kaibla.hamster.base.HamsterSession;
import com.kaibla.hamster.base.UIEngine;
import com.kaibla.hamster.util.Template;

/**
 * 
 * @author korend
 */
public class ExampleChatPage extends HamsterPage {

    /**
     * HTML Template for example chat page
     */
    private static transient Template PAGE = new Template(ExampleChatPage.class.
            getResource("page.html"));

    public ExampleChatPage() {
    }

    //our chat component
    Chat chat;

    public ExampleChatPage(UIEngine engine, HamsterSession session) {
        super(engine, session);
        //set HTML Template
        setTemplate(PAGE);
        //Set a Title prefix
        setTitlePrefix("Hamster Framework");
        //set the title
        setTitle("Example Chat");
    }

    @Override
    public void init() {
        //initiliaze the page content
        super.init();
        //create new chat UI component
        this.chat = new Chat(page);
        //add the chat to this page
        addComponent(chat);
    }

    @Override
    public String getContextRoot() {
        //we have to overwrite this
        //as it is not possible to determine the context root in jetty programmatically
        return "/example-chat/";
    }

}
