package lk.opdqueue.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
@ConditionalOnProperty(name = "app.r2.enabled", havingValue = "true")
public class R2StorageService {

    private final S3Client r2Client;

    @Value("${app.r2.bucket-name}")
    private String bucketName;

    @Value("${app.r2.public-url}")
    private String publicUrl;

    public R2StorageService(S3Client r2Client) {
        this.r2Client = r2Client;
    }

    public String uploadPdf(String key, byte[] data) {
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType("application/pdf")
                .build();
        r2Client.putObject(request, RequestBody.fromBytes(data));
        return publicUrl + "/" + key;
    }

    public void delete(String key) {
        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();
        r2Client.deleteObject(request);
    }

    public String getPublicUrl(String key) {
        return publicUrl + "/" + key;
    }
}