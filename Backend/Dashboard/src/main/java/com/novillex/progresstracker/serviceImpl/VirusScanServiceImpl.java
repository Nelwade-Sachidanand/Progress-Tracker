package com.novillex.progresstracker.serviceImpl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.novillex.progresstracker.common.ErrorCode;
import com.novillex.progresstracker.exception.ValidationException;
import com.novillex.progresstracker.service.VirusScanService;

@Service
public class VirusScanServiceImpl implements VirusScanService {

	private static final Logger logger = LoggerFactory.getLogger(VirusScanServiceImpl.class);

	@Value("${virus.scan.enabled}")
	private boolean virusScanEnabled;

	@Value("${virus.scan.host}")
	private String clamHost;

	@Value("${virus.scan.port}")
	private int clamPort;

	@Override
	public void scan(MultipartFile file) {

		if (!virusScanEnabled) {

			logger.info("Virus scan is disabled.");

			return;
		}

		logger.info("Scanning file : {}", file.getOriginalFilename());

		try (Socket socket = new Socket(clamHost, clamPort);
				InputStream fileInputStream = file.getInputStream();
				OutputStream outputStream = socket.getOutputStream();
				InputStream inputStream = socket.getInputStream()) {

			outputStream.write("zINSTREAM\0".getBytes());
			outputStream.flush();

			byte[] buffer = new byte[2048];

			int bytesRead;

			while ((bytesRead = fileInputStream.read(buffer)) != -1) {

				outputStream.write(ByteBuffer.allocate(4).putInt(bytesRead).array());

				outputStream.write(buffer, 0, bytesRead);
			}

			outputStream.write(new byte[] { 0, 0, 0, 0 });

			outputStream.flush();

			byte[] response = new byte[1024];

			int length = inputStream.read(response);

			if (length <= 0) {
				throw new ValidationException(ErrorCode.VIRUS_SCAN_FAILED, "No response received from ClamAV.");
			}

			String result = new String(response, 0, length);

			logger.info("ClamAV Response : {}", result);

			if (!result.contains("OK")) {

				logger.error("Virus detected in file : {}", file.getOriginalFilename());

				throw new ValidationException(ErrorCode.VIRUS_FOUND, "Uploaded document contains malware.");
			}

			logger.info("Virus scan completed successfully.");

		} catch (IOException ex) {

			logger.error("Unable to scan uploaded document.", ex);

			throw new ValidationException(ErrorCode.VIRUS_SCAN_FAILED, "Unable to scan uploaded document.");
		}

	}

}