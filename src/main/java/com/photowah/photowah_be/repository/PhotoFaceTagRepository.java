package com.photowah.photowah_be.repository;

import com.photowah.photowah_be.entity.PhotoFaceTag;
import com.photowah.photowah_be.entity.PhotoFaceTagId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface PhotoFaceTagRepository extends JpaRepository<PhotoFaceTag, PhotoFaceTagId> {

    @Query("SELECT pft FROM PhotoFaceTag pft JOIN FETCH pft.photo WHERE pft.id.faceTagId = :faceTagId")
    List<PhotoFaceTag> findByFaceTagId(@Param("faceTagId") UUID faceTagId);
}
