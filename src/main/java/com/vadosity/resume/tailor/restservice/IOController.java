package com.vadosity.resume.tailor.restservice;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vadosity.resume.tailor.DocxUpdate;
import com.vadosity.resume.tailor.DocxUpdateRequest;
import com.vadosity.resume.tailor.KeywordMatchResults;

@RestController
@RequestMapping("/resume")
public class IOController {
	
	private ObjectMapper objectMapper= new ObjectMapper();
	
	@Value("${outputDocumentSuffix}")
	private String outputDocumentSuffix;
	
	@Value("${inputDocumentSuffix}")
	private String inputDocumentSuffix;
	
	@Value("${resumeOutputDirectory}")
	private String resumeOutputDirectory;	
	
	@RequestMapping(path="/test", method=RequestMethod.GET)
	public ResponseEntity<String> testMessage()
	{
		return ResponseEntity.ok(resumeOutputDirectory);
	}
	
	@RequestMapping(path = "/download", method = RequestMethod.GET)
    public ResponseEntity<Resource> download(@RequestParam("filename") String filename) throws IOException {
		verifyResumeOutputDirectory();
		
		
        File file = new File(resumeOutputDirectory + filename);

        HttpHeaders header = new HttpHeaders();
        header.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename="+filename);
        header.add("Cache-Control", "no-cache, no-store, must-revalidate");
        header.add("Pragma", "no-cache");
        header.add("Expires", "0");

        Path path = Paths.get(file.getAbsolutePath());
        ByteArrayResource resource = new ByteArrayResource(Files.readAllBytes(path));

        return ResponseEntity.ok()
                .headers(header)
                .contentLength(file.length())
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .body(resource);
    }
	
		
	@PostMapping("/upload")
	public ResponseEntity<KeywordMatchResults> upload(@RequestPart("file") MultipartFile multiPartFile,
			@RequestParam(value = "json", defaultValue = "{}") String json) {
		verifyResumeOutputDirectory();
		
		if (null == multiPartFile.getOriginalFilename()) {
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
		
		try {
			/// get the request from json
			DocxUpdateRequest docxUpdateRequest= objectMapper.readValue(json, DocxUpdateRequest.class);
			
			String originalDocumentPath = persistInputDocument(multiPartFile);
			String updatedDocumentPath= determineUpdatedDocumentPath(originalDocumentPath);
			
			File in  = new File(originalDocumentPath);
			File out = new File(updatedDocumentPath);

			DocxUpdate docxUpdate =  new DocxUpdate();
			docxUpdate.updateDocument(in, out, docxUpdateRequest);
			KeywordMatchResults keywordMatchResults=  docxUpdate.getKeywordMatchResults();
			keywordMatchResults.setOutputFilename(updatedDocumentPath.substring(updatedDocumentPath.lastIndexOf(File.separator)+1));
			keywordMatchResults.setPercentageMatch(keywordMatchResults.determinePercentageMatched());
			
			System.out.println("updated document written to: " +updatedDocumentPath);
			return ResponseEntity.ok(keywordMatchResults);
		} catch (Exception e) {
			System.out.println(e.getMessage());
			return ResponseEntity.notFound().build();
		}		
				
	}
	
	private String determineUpdatedDocumentPath(String originalDocumentPath) {
		String updatedPath = originalDocumentPath.substring(0,originalDocumentPath.lastIndexOf(inputDocumentSuffix));
		updatedPath+=outputDocumentSuffix;
		return updatedPath;
	}
	
	/** saves the attachment to the file system.
	 * 
	 * @param multiPartFile
	 * @return the absolute path to the file
	 * @throws IOException
	 */
	private String persistInputDocument(MultipartFile multiPartFile) throws IOException {
		byte[] bytes = multiPartFile.getBytes();
		
		String origName=multiPartFile.getOriginalFilename();
		String outputFilename=origName.substring(0,origName.lastIndexOf(".docx"));
		outputFilename+=inputDocumentSuffix;
		String outputFilepath=resumeOutputDirectory+outputFilename;
		Path path = Paths.get(outputFilepath);
		Files.write(path, bytes,StandardOpenOption.CREATE,
		         StandardOpenOption.TRUNCATE_EXISTING);
		return outputFilepath;
	}
	
	private void verifyResumeOutputDirectory() {
		String dir = resumeOutputDirectory;
		while ( dir.endsWith(File.separator)) {
			dir=dir.substring(0,dir.lastIndexOf(File.separator));
		}
		File f = new File(dir);
		if ( !f.exists()) f.mkdirs();
		
	}
	
}
