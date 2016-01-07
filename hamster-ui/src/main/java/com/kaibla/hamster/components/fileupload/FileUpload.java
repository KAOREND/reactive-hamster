/*
 * Registered.java
 *
 * Created on 15. August 2007, 18:47
 */
package com.kaibla.hamster.components.fileupload;

import com.kaibla.hamster.base.Action;
import com.kaibla.hamster.base.UIContext;
import com.kaibla.hamster.base.HamsterComponent;
import com.kaibla.hamster.base.HamsterPage;
import com.kaibla.hamster.util.Template;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.MediaTracker;
import static java.awt.Toolkit.getDefaultToolkit;
import java.io.File;
import java.util.LinkedList;
import static java.util.logging.Logger.getLogger;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author Kai Orend
 */
public abstract class FileUpload extends HamsterComponent {

    FileUpload self = this;
    boolean loadingUp = false;
    int progress = 0;
    String info = "";
    private String buttonText = "Datei hochladen";
    ProgressContainer container;
    private boolean multi = true;
    private boolean absoluteFilePath = false;
    private Template form = IFRAME;
    private Template progressTemplate = PROGRESS;

    public FileUpload(HamsterPage page) {
        super(page);
        container = new ProgressContainer(page);
        addComponent(container);
        buttonText = "Datei hochladen";
    }

    /**
     * Creates a new instance of Registered
     */
    public FileUpload() {
    }

    public Template getProgressTemplate() {
        return progressTemplate;
    }

    public void setProgressTemplate(Template progressTemplate) {
        this.progressTemplate = progressTemplate;
    }

    public void setForm(Template form) {
        this.form = form;
    }

    @Override
    public void markForUpdate() {
        //super.markForUpdate();
    }

    public void setAbsoluteFilePath(boolean absoluteFilePath) {
        this.absoluteFilePath = absoluteFilePath;
    }

    @Override
    public void generateHTMLCode() {

        LinkedList slots = new LinkedList();
        slots.add("" + getId());
        slots.add(container);
        slots.add(getIFrameURL());
        htmlCode = FILE_UPLOAD.mergeStrings(slots, getPage());

    }

    @Override
    public String getIFrameHTMLCode() {
        LinkedList slots = new LinkedList();
        slots.add(getUploadAction(new FileUploadAction()));
        slots.add(getButtonText());
        return form.mergeStrings(slots, getPage());
    }

    /**
     *
     * @return The Path in which the File should be uploaded
     */
    public abstract String getUploadPath();

    /**
     * Override this method if the real directory is not the same as the upload path.
     *
     * @return
     */
    public File getUploadDir() {
        return new File(getPage().getEngine().getServlet().getServletContext().getRealPath(getUploadPath()));
    }

    /**
     * Is called when the upload of a file is finished
     *
     * @param file The uploaded File
     */
    public abstract void postUpload(File file);

    /**
     * Is called before the upload starts.
     */
    public void preUpload() {
    }

    public void finished() {
    }

    public void progressChanged(int progress) {
    }

    public static Dimension getImageDimension(File f) {
        Image img = getDefaultToolkit().getImage(f.getAbsolutePath());
        Container c = new Container();
        MediaTracker mediaTracker = new MediaTracker(c);
        mediaTracker.addImage(img, 0);
        try {
            mediaTracker.waitForID(0);
        } catch (InterruptedException ex) {
            LOG.log(java.util.logging.Level.SEVERE, ex.getMessage(), ex);
        }
        return new Dimension(img.getWidth(c), img.getHeight(c));
    }

    /**
     * @return the buttonText
     */
    public String getButtonText() {
        return buttonText;
    }

    /**
     * @param buttonText the buttonText to set
     */
    public void setButtonText(String buttonText) {
        this.buttonText = buttonText;
    }

    /**
     * @return the multi
     */
    public boolean isMulti() {
        return multi;
    }

    /**
     * @param multi the multi to set
     */
    public void setMulti(boolean multi) {
        this.multi = multi;
    }

    public class FileUploadAction extends Action {

        @Override
        public void invoke() {
            preUpload();
            ProgressBar bar = new ProgressBar(getPage(), self);
            container.addComponent(bar);
//            lock();
            bar.handUpload(UIContext.getRequest());
//            unlock();
        }
    }
    private static transient Template FILE_UPLOAD = new Template(FileUpload.class.getResource("fileupload.html"));
    private static transient Template IFRAME = new Template(FileUpload.class.getResource("fileupload_iframe.html"));
    private static transient Template PROGRESS = new Template(FileUpload.class.getResource("progress.html"));

    private static final java.util.logging.Logger LOG = getLogger(FileUpload.class.getName());
}
