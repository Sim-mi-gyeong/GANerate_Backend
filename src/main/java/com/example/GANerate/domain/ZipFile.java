package com.example.GANerate.domain;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ZipFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "zipfile_id")
    private Long id;

    @NotNull
    private String originalFileName;

    @NotNull
    private String uploadFileName;

    @NotNull
    private int size;

    @NotNull
    @Column(length = 15000)
    private String uploadUrl;

    @Builder
    public ZipFile(Long id, String originalFileName, String uploadFileName, int size, String uploadUrl) {
        this.id = id;
        this.originalFileName = originalFileName;
        this.uploadFileName = uploadFileName;
        this.size = size;
        this.uploadUrl = uploadUrl;
    }
}
