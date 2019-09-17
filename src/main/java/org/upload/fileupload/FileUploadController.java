package org.finra.fileupload;

import java.io.IOException;
import java.io.InputStream;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.fileupload.FileItemIterator;

import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.joda.time.DateTime;
import org.joda.time.Minutes;
import org.joda.time.Seconds;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

/**
 * Apache commons file upload method will be used
 */
@Controller
public class FileUploadController
{
    private AmazonClient amazonClient;

    @Autowired
    public FileUploadController(AmazonClient amazonClient)
    {
        this.amazonClient = amazonClient;
    }

    @PostMapping(value = "/upload")
    public String handleFileUpload(HttpServletRequest request)
    {
        DateTime start = new DateTime();
        DateTime end = new DateTime();
        boolean isFound = false;

        // Check that we have a file upload request
        boolean isMultipart = ServletFileUpload.isMultipartContent(request);

        if (isMultipart)
        {
            // Create a new file upload handler
            ServletFileUpload servletFileUpload = new ServletFileUpload();
            try
            {
                // Parse the request
                FileItemIterator fileItemIterator = servletFileUpload.getItemIterator(request);
                start = DateTime.now();
                while (fileItemIterator.hasNext())
                {
                    FileItemStream item = fileItemIterator.next();
                    if (!item.isFormField())
                    {
                       isFound = amazonClient.uploadToAmazon(item);
					   
					   //Uploading to a local directory
//					      InputStream uploadedStream = item.openStream();
//                        OutputStream outputStream = new FileOutputStream("C:\\Users\\David\eclipse-workspace\\fileUploadPoc\\uploads\\" + name);
//                        {
//                            IOUtils.copy(uploadedStream, outputStream);
//                            outputStream.close();
//                            inputStream.close();
//                        }
                    }
                }
                end = DateTime.now();
            }
            catch (IOException | FileUploadException ex)
            {
                ex.printStackTrace();
            }
        }
        if (isFound)
        {
            request.setAttribute("timeTaken",
                Minutes.minutesBetween(start, end).getMinutes()+ " minutes " + Seconds.secondsBetween(start, end).getSeconds() % 60 + " seconds.");
            return "success";
        }
        else
        {
            return "unsuccessful";
        }
    }

    @RequestMapping("/uploader")
    public String uploaderPage(ModelAndView modelAndView)
    {
        return "uploader";
    }
}
