package com.bonc.bcos.service.tasks;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.net.ftp.FTPClient;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class FTPUtil{
    public static final String PACKAGE_LISTENER_KEY = "PACKAGE_LISTENER_KEY";

    private FTPListener listener;
    private FTPClient client;
    private String path;
    private InputStream stream;

    private static String packagePath = "package/";

    public FTPListener getListener(){
        return listener;
    }

    public FTPUtil(MultipartFile file) throws IOException {
        client = new FTPClient();
        listener = new FTPListener(file.getSize(),this);
        this.path = packagePath+file.getOriginalFilename();
        this.stream = file.getInputStream();
    }

    public boolean connect(String hostname, String ftpPort) {
        int port = 21;
        if (!StringUtils.isEmpty(ftpPort)){
            port = Integer.parseInt(ftpPort);
        }
        try {
            client.connect(hostname, port);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean login(String username, String password) throws IOException {
        return client.login(username, password);
    }

    void logout() throws IOException {
        client.logout();
    }

    void quit() throws IOException {
        client.quit();
    }

    public void send() throws IOException {
        client.setFileType(FTPClient.BINARY_FILE_TYPE);
        int n;
        client.setBufferSize(100*1024);
        client.enterLocalPassiveMode();

        client.makeDirectory(packagePath);

        int bufferSize = client.getBufferSize();
        byte[] buffer = new byte[bufferSize];

        OutputStream outputstream = client.storeFileStream(path);
        while ((n = stream.read(buffer)) != -1) {
            outputstream.write(buffer);
            listener.update(n);
        }
        stream.close();
        outputstream.flush();
        outputstream.close();
        logout();
        quit();
        listener.finish(true);
    }

}