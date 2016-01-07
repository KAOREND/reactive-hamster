package com.kaibla.hamster.example.ui;

import com.kaibla.hamster.base.DataEvent;
import com.kaibla.hamster.base.DataModel;
import com.kaibla.hamster.base.HamsterComponent;
import com.kaibla.hamster.base.HamsterPage;
import com.kaibla.hamster.base.UIContext;
import com.kaibla.hamster.components.defaultcomponent.DefaultComponent;
import com.kaibla.hamster.components.form.Form;
import com.kaibla.hamster.components.form.InputField;
import com.kaibla.hamster.components.form.TextField;
import com.kaibla.hamster.components.list.List;
import com.kaibla.hamster.example.persistence.DocumentCollections;
import com.kaibla.hamster.example.persistence.Messages;
import com.kaibla.hamster.example.persistence.Users;
import com.kaibla.hamster.persistence.model.Document;
import com.kaibla.hamster.util.Template;

/**
 * This is the UI Component in our example which contains the Chat.
 *
 *
 * @author korend
 */
public class Chat extends DefaultComponent {

    /**
     * HTML Template for the Chat
     */
    private static transient Template CHAT = new Template(Chat.class.
            getResource("chat.html"));

    /**
     * HTML Template for a single chat message
     */
    private static transient Template MESSGAE = new Template(Chat.class.
            getResource("message.html"));

    /**
     * HTML Template for the list of chat messages
     */
    private static transient Template MESSGAES = new Template(Chat.class.
            getResource("messages.html"));

    private static final long serialVersionUID = 1L;

    /**
     * UI List Component containing our messages
     */
    List messageList;

    public Chat() {
    }

    public Chat(HamsterPage page) {
        //create this component with CHAT as Template and configure it so that each SLOT in the Template represents only a single component
        super(page, false, CHAT);

    }

    @Override
    public void onShow() {
        //scroll down the message List 
        scrollDown(messageList.getId());
        super.onShow();
    }

    @Override
    /**
     * The init method will be called by the framework for initializing the UI Component tree of this Component. Here we
     * can initialize all our child UI Components
     */
    public void init() {

        super.init();

        //create a UI List Component containing all our chat message
        messageList = new List(page, //the page this component is beeing used in
                "messages", //we give this list component the name messages
                DocumentCollections.MESSAGES.getMessages(this), //here we tell the list component where to get the content
                20, //this will limit the inital number of entries to 20
                true, //setting this to true reverts the list order, which is useful for our chat
                true //activate endless scrolling (if the users scrolls the list component will load more entries)
        ) {

            @Override
            /**
             * Here we have to define how a list entry (in our case a chat message) has to be rendered. It will return a
             * UI Component for each list entry.
             */
            public HamsterComponent renderElement(DataModel data) {
                Document<Messages> message = (Document<Messages>) data;
                Document<Users> author = message.get(Messages.USER);
                if (author == null) {
                    author = Users.DEFAULT_USER;
                }
                //Just create as simple component for displaying the message using our MESSAGE Template
                DefaultComponent messageBox = new DefaultComponent(page, false, MESSGAE);
                //fill in the values for the Tempalte SLOTs:
                messageBox.addElement(author.getStringSource(Users.NAME)); //we get the Name of the User as StringSource, so that it can get updated 
                messageBox.addElement(message.getStringSource(Messages.TEXT));  //we get the Text of the User as StringSource, so that it can get updated 

                //now we have to bind our message component to events, so that it gets update if the content changes
                messageBox.acquireDataModel(message);              //update messageBox when the message should be changed
                messageBox.acquireDataModel(author, Users.NAME);  //update messageBox when the name of the Author changes

                return messageBox;
            }

            @Override
            public void dataChanged(DataEvent e) {
                //list content has changed, so scroll down
                scrollDown(getId());
                super.dataChanged(e);
            }

        };

        //set the HTML Template for the messageList
        messageList.setTemplate(MESSGAES);
        //add the messageList to the Chat Component.
        this.addElement(messageList);

        //create Form for changing the user name:        
        Form userNameForm = new Form(page) {
            @Override
            public void evaluate() {
                //this will be called when the user changed the content of the input field
                //we just update the name of the user:
                UIContext.getUser().set(Users.NAME, getString("name")); //get the field "name" from the form and store it as name our user
                UIContext.getUser().writeToDatabase();  //write the changes to the db and fires the change events
            }
        };
        //create an input text field for changing the user name:
        InputField nameInput = new InputField(page, "name", UIContext.getUser().get(Users.NAME))
                .setSubmitOnKeyUp(true); //we want to update the name as soon as the user stops typing
        userNameForm.addElement(nameInput); // add this input field to the form
        this.addElement(userNameForm);   // add the form to our chat component

        //create another form for writing the chat messages
        //create a text field for entering the message with the fild name "text"
        final TextField messageInput = new TextField(page, "text", "");
        //create the form:
        Form messageForm = new Form(page) {
            @Override
            public void evaluate() {
                //this method is invoked when the form is beeing submitted
                //create the new message, get the field value text from the form this
                DocumentCollections.MESSAGES.addMessage(getString("text"));
                //we want to focus again on the message TextField
                messageInput.forceFocus();
            }
        };
        //try to get a conintonousFocus with Priority 0 on the text field for the message
        messageInput.requestContinousFocus(0);
        //set css class for styling
        messageInput.setCSSClass("textbox");
        //add the text field to the form
        messageForm.addElement(messageInput);
        //create a simple submit button
        messageForm.addSubmitButton("Send");
        //after submiting the form the values in the form should be reset 
        messageForm.setResetAfterSending(true);
        this.addElement(messageForm);
        
        //scroll down the message list
        scrollDown(messageList.getId());
    }

    /**
     * Uses jQuery to scroll down the message area
     *
     * @param id Id of the message list
     */
    private void scrollDown(String id) {
        //jQuery JavaScript on the client to scroll down
        //with an animation
        exec("$(\"#"
                + id
                + "\").animate({\n"
                + "  scrollTop: $('#"
                + id
                + "')[0].scrollHeight - $('#"
                + id
                + "')[0].clientHeight\n"
                + "}, 300)");
    }
}
