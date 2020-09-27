package com.bonc.bcos.service.service;

import com.bonc.bcos.service.entity.SysClusterVersion;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;

public interface VersionService {

    void saveUploadPackage(MultipartFile file, HttpSession session,String code) throws IOException;

    void saveVersion(SysClusterVersion parseObject);

    List<SysClusterVersion> getPack();

    void deletePack(String packageName);
}
