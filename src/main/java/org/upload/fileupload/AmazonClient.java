package org.finra.fileupload;

import static com.sun.xml.internal.ws.spi.db.BindingContextFactory.LOGGER;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Executors;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.event.ProgressEvent;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import com.amazonaws.services.s3.transfer.Upload;
import org.apache.commons.fileupload.FileItemStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AmazonClient
{
    private AmazonS3 s3Client;

    private ObjectMetadata metadata;

    @Value("${amazonProperties.endpointUrl}")
    private String endpointUrl;

    @Value("${amazonProperties.bucketName}")
    private String bucketName;


    public boolean uploadToAmazon(FileItemStream item)
    {
        boolean result = false;
        try
        {
            this.s3Client = AmazonS3ClientBuilder.standard().build();
            String key = "CATDD/" + item.getName();

            //Configuring a transfer manager with s3 client
            TransferManager transferManager = TransferManagerBuilder.standard()
                .withS3Client(s3Client)
                .withMultipartUploadThreshold((long) (5 * 1024 * 1024))
                .withMinimumUploadPartSize((long) (5 * 1024 * 1024))
//                .withExecutorFactory(() -> Executors.newFixedThreadPool(5))               //default max 10 threads. Used if size is >15Mb
                .build();

            InputStream uploadedStream = item.openStream();

            ObjectMetadata metadata = new ObjectMetadata();

            //Read the contents into a byte array(loaded into the memory, which we do not want because we want to stream it directly)
//          byte[] bytes = IOUtils.toByteArray(uploadedStream);
//          metadata.setContentLength(bytes.length);
//          ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);

            metadata.setContentType(item.getContentType());

            Upload upload = transferManager.upload(bucketName, key, uploadedStream, metadata);

            //Registering a progress listener
            upload.addProgressListener((ProgressEvent progressEvent) -> {
                LOGGER.info("Progress: " + progressEvent);
            });

            try
            {
                upload.waitForCompletion();
                result = true;
            }
            catch (InterruptedException e)
            {
                LOGGER.warning("Failed to upload file: " + e.getLocalizedMessage());
            }

            transferManager.shutdownNow();
        }
        catch (AmazonServiceException | IOException ase)
        {
            ase.printStackTrace();
        }

        return result;
    }
}
