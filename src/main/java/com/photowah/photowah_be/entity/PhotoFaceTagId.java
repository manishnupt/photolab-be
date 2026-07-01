package com.photowah.photowah_be.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class PhotoFaceTagId implements Serializable {

    @Column(name = "photo_id", nullable = false)
    private UUID photoId;

    @Column(name = "face_tag_id", nullable = false)
    private UUID faceTagId;
}
