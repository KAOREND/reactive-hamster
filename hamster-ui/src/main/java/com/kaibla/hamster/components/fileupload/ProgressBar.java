/*
 * Registered.java
 *
 * Created on 15. August 2007, 18:47
 */
package com.kaibla.hamster.components.fileupload;

import com.kaibla.hamster.base.HamsterComponent;
import com.kaibla.hamster.base.HamsterPage;
import static com.kaibla.hamster.util.HTMLCodeFilter.getStrictFilteredString;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import static java.lang.Math.round;
import static java.lang.Thread.sleep;
import java.util.Formatter;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import static java.util.logging.Logger.getLogger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

/**
 *
 * @author Kai Orend
 */
public class ProgressBar extends HamsterComponent {

    boolean loadingUp = false;
    int progress = 0;
    int oldProgress = 0;
    String info = "";
    File dir;
    FileUpload uploader;

    public ProgressBar(HamsterPage page, FileUpload uploader) {
        super(page);
        this.uploader = uploader;
    }

    /**
     * Creates a new instance of Registered
     */
    public ProgressBar() {
    }

    @Override
    public void generateHTMLCode() {
        String[] slots = new String[2];
        slots[0] = "" + getId();
        slots[1] = "" + info;
        // slots[2] = ""+progress;
        LOG.log(Level.INFO, "progress: {0}", progress);
        htmlCode = uploader.getProgressTemplate().mergeStrings(slots, getPage());
    }

    public void handUpload(HttpServletRequest request) {
        loadingUp = true;
        boolean noZip = true;
        markForUpdate();
//	    exec("hamster.mainAnim.showAJAXLoader()");
        //      exec("MA.LOADER.setAJAXLoaderText(\"lade Datei hoch\")");
        //  exec("hamster.main.refreshTime=250");
        LOG.log(Level.INFO, "FileUploadAction invoked: {0}", request);

        DiskFileItemFactory factory = new DiskFileItemFactory();
        factory.setSizeThreshold(16384);
        factory.setRepository(new File("/tmp"));
        ServletFileUpload upload = new ServletFileUpload(factory);
        //upload.setSizeMax(10000000);

        try {
            //List files = diskFileUpload.parseRequest(request);
            List files = upload.parseRequest(request);
            byte[] buffer = new byte[16384];
            for (Iterator iter = files.iterator(); iter.hasNext();) {
                FileItem element = (FileItem) iter.next();
                if (!element.isFormField()) {
                    String fileName = element.getName();
                    fileName = fileName.replace('\\', '/');
                    fileName = fileName.substring(fileName.lastIndexOf('/') + 1);
                    fileName = getStrictFilteredString(fileName.replace(' ', '1')).replace('%', '_').toLowerCase();

                    InputStream is = element.getInputStream();
                    dir = uploader.getUploadDir();
                    dir.mkdirs();
                    //uploader.setDir(dir);

                    if (uploader.isMulti() && element.getName().endsWith(".zip")) {
                        noZip = false;
                        try {
                            ZipInputStream zipstream = new ZipInputStream((is));

                            ZipEntry entry = zipstream.getNextEntry();
                            LOG.info("FileUpload: ZipStream detected");

                            long pr = 0;
                            while (entry != null) {
                                if (!entry.isDirectory()) {
                                    fileName = entry.getName();
                                    fileName = fileName.replace('\\', '/');
                                    fileName = fileName.substring(fileName.lastIndexOf('/') + 1);
                                    fileName = getStrictFilteredString(fileName.replace(' ', '1')).replace('%', '_').toLowerCase();

                                    File f = new File(dir.getAbsolutePath(), fileName);
                                    LOG.log(Level.INFO, "upload File: {0} ", f.getAbsolutePath());
                                    f.createNewFile();
                                    //			element.write(f);
                                    FileOutputStream fos = new FileOutputStream(f);

                                    LOG.log(Level.INFO, "upload File: {0} ", fileName);
                                    Formatter formatter = new Formatter(Locale.GERMAN);
                                    Object mb[] = {new Float((element.getSize() / (1024.0f * 1024.0f)))};

                                    info = getStrictFilteredString(element.getName()) + " (" + formatter.format("%10.1f", mb) + "MB)";
                                    int len = 0;

                                    float factor = 1.0f / element.getSize();

                                    while ((len = zipstream.read(buffer, 0, 16384)) > 0) {
                                        fos.write(buffer, 0, len);
                                        // System.out.print(buffer);
                                        // System.out.write(buffer, 0, len);
                                        pr += len;
                                        progress = round(pr * factor * 100);
                                        //markForUpdate();
                                        if (progress != oldProgress) {
                                            // exec("hamster.mainAnim.changeUploadStatusProgress('uploadstatusID', " + progress + ")");
                                            // Thread.sleep(100);
                                            oldProgress = progress;
                                            uploader.progressChanged(progress);
                                            info = encode("" + progress + "%");
                                            markForUpdate();
                                        }
                                        // LOG.info("file upload progress: "
                                        // + progress + " done:" + pr + " of "
                                        // + element.getSize() + " factor: " + factor);

                                    }
                                    fos.flush();
                                    fos.close();
                                    //is.close();
                                    LOG.info("fileupload fertig");
                                    //exec("hamster.mainAnim.showAJAXLoader()");
                                    // exec("MA.LOADER.setAJAXLoaderText(\"bearbeite Datei...\")");
                                    uploader.postUpload(f);
                                }
                                zipstream.closeEntry();
                                entry = zipstream.getNextEntry();
                            }
                            zipstream.close();
                            loadingUp = false;
                            uploader.finished();
                            markForUpdate();

                        } catch (IOException ex) {
                            LOG.log(java.util.logging.Level.SEVERE, ex.getMessage(), ex);

                        }
                    }

                    if (noZip) {

                        File f = new File(dir.getAbsolutePath(), fileName);
                        LOG.log(Level.INFO, "upload File: {0} ", f.getAbsolutePath());
                        f.createNewFile();
                        //			element.write(f);
                        FileOutputStream fos = new FileOutputStream(f);

                        LOG.log(Level.INFO, "upload File: {0} ", fileName);
                        Formatter formatter = new Formatter(Locale.GERMAN);
                        Object mb[] = {new Float((element.getSize() / (1024.0f * 1024.0f)))};

                        info = fileName + " (" + formatter.format("%10.1f", mb) + "MB)";
                        int len = 0;
                        long pr = 0;
                        float factor = 1.0f / element.getSize();
                        while ((len = is.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                            // System.out.print(buffer);
                            // System.out.write(buffer, 0, len);
                            pr += len;
                            progress = round(pr * factor * 100);
                            //markForUpdate();
                            if (progress != oldProgress) {
                                // exec("hamster.mainAnim.changeUploadStatusProgress('uploadstatusID', " + progress + ")");
                                //Thread.sleep(100);
                                oldProgress = progress;
                                uploader.progressChanged(progress);
                                info = encode("" + progress + "%");
                                markForUpdate();
                            }
                            // LOG.info("file upload progress: "
                            // + progress + " done:" + pr + " of "
                            // + element.getSize() + " factor: " + factor);
                            sleep(100);
                        }
                        fos.flush();
                        fos.close();
                        is.close();
                        LOG.info("fileupload fertig");

                        //exec("hamster.mainAnim.showAJAXLoader()");
                        //exec("MA.LOADER.setAJAXLoaderText(\"bearbeite Datei...\")");
                        uploader.postUpload(f);
                        loadingUp = false;
                        uploader.finished();
                        markForUpdate();
                    }
                }
            }
        } catch (FileUploadException ex) {
            LOG.log(java.util.logging.Level.SEVERE, ex.getMessage(), ex);
            exec("alert('" + ex.getMessage() + "')");
            loadingUp = false;
            markForUpdate();

        } catch (IOException e) {
            LOG.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
            loadingUp = false;
            markForUpdate();
        } catch (InterruptedException e) {
            LOG.log(java.util.logging.Level.SEVERE, e.getMessage(), e);
            loadingUp = false;
            markForUpdate();
        }
        //     exec("hamster.main.refreshTime=2000");
//	    exec("hamster.mainAnim.hideAJAXLoader()");
        //      exec("MA.LOADER.setAJAXLoaderText(\"Hochladen fertig\")");
        uploader.container.removeAndDestroy(this);
    }
    private static final Logger LOG = getLogger(ProgressBar.class.getName());

}
