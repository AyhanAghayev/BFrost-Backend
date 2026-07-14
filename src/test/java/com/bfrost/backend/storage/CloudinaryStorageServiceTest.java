package com.bfrost.backend.storage;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CloudinaryStorageServiceTest {

    @Test
    void storeUploadsBytesAndReturnsSecureUrl() throws Exception {
        Cloudinary cloudinary = mock(Cloudinary.class);
        Uploader uploader = mock(Uploader.class);
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(byte[].class), any()))
                .thenReturn(Map.of("secure_url", "https://res.cloudinary.com/demo/image/upload/v1/avatars/abc.png"));
        CloudinaryStorageService service = new CloudinaryStorageService(cloudinary);
        MockMultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", "data".getBytes());

        String url = service.store(file, "avatars");

        assertThat(url).isEqualTo("https://res.cloudinary.com/demo/image/upload/v1/avatars/abc.png");
    }

    @Test
    void deleteDestroysThePublicIdParsedFromTheUrl() throws Exception {
        Cloudinary cloudinary = mock(Cloudinary.class);
        Uploader uploader = mock(Uploader.class);
        when(cloudinary.uploader()).thenReturn(uploader);
        CloudinaryStorageService service = new CloudinaryStorageService(cloudinary);

        service.delete("https://res.cloudinary.com/demo/image/upload/v1699999999/clubs/abc123.png");

        verify(uploader).destroy(eq("clubs/abc123"), any());
    }

    @Test
    void extractsPublicIdStrippingVersionAndExtension() {
        assertThat(CloudinaryStorageService.extractPublicId(
                "https://res.cloudinary.com/demo/image/upload/v1/avatars/abc.png"))
                .isEqualTo("avatars/abc");
        assertThat(CloudinaryStorageService.extractPublicId("not-a-cloudinary-url"))
                .isNull();
    }
}
