package com.photowah.photowah_be.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "photo_face_tag")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"photo", "faceTag"})
public class PhotoFaceTag {

    @EmbeddedId
    private PhotoFaceTagId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("photoId")
    @JoinColumn(name = "photo_id", nullable = false)
    private Photo photo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("faceTagId")
    @JoinColumn(name = "face_tag_id", nullable = false)
    private FaceTag faceTag;

    @Column(nullable = false)
    private Double similarityScore;
}
