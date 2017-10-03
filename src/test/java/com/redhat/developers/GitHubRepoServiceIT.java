/**
 * Copyright (c) 2017 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.redhat.developers;

import com.redhat.developers.config.CheBootializrConfiguration;
import com.redhat.developers.service.GitHubRepoService;
import com.redhat.developers.vo.RepoVO;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, classes = CheBootializrConfiguration.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class GitHubRepoServiceIT {

    @Autowired
    GitHubRepoService gitHubRepoService;

    @Rule
    public TemporaryFolder gitRepoFolder = new TemporaryFolder();

    @Test
    public void should_create_a_repo_called_demo() throws Exception {
        assertThat(gitHubRepoService).isNotNull();
        gitHubRepoService.createRepo("demo", "my demo repo");
    }

    @Test
    public void should_delete_a_repo_called_demo() throws Exception {
        assertThat(gitHubRepoService).isNotNull();
        boolean isDeleted = gitHubRepoService.deleteRepo("demo");
        assertThat(isDeleted).isTrue();
    }

    @Test
    public void will_not_delete_repo() throws Exception {
        assertThat(gitHubRepoService).isNotNull();
        boolean isDeleted = gitHubRepoService.deleteRepo("demo2");
        assertThat(isDeleted).isFalse();
    }

    @Test
    public void should_get_repo_getdemo() throws Exception {
        assertThat(gitHubRepoService).isNotNull();
        Optional<RepoVO> createdRepo = gitHubRepoService.createRepo("getdemo", "my get demo repo");
        assertThat(createdRepo.isPresent()).isTrue();

        await().atMost(10, TimeUnit.SECONDS)
            .until(() -> {
                //Awaitality
                Optional<RepoVO> getRepo = gitHubRepoService.getRepo("getdemo");
                assertThat(getRepo.isPresent()).isTrue();

                assertThat(createdRepo.get().getName()).isEqualTo(getRepo.get().getName());
                assertThat(createdRepo.get().getFqdn()).isEqualTo(getRepo.get().getFqdn());

                //Cleanup
                return gitHubRepoService.deleteRepo("getdemo");
            });
    }

    @Test
    public void should_add_content_to_repo_demoadd() throws Exception {

        //delete any testing repo
        gitHubRepoService.deleteRepo("demoadd");

        //Add dummy file
        File myFile = gitRepoFolder.newFile("README.md");
        myFile.createNewFile();
        myFile.setWritable(true);
        Files.write(Paths.get(myFile.toURI()), "Hello World!!!".getBytes());

        assertThat(myFile).isFile();
        assertThat(myFile).exists();

        String fileContent = new String(Files.readAllBytes(Paths.get(myFile.toURI())));

        assertThat(fileContent).isEqualToIgnoringWhitespace("Hello World!!!");

        Optional<RepoVO> repoVO = gitHubRepoService.createRepo("demoadd","Test repo created");

        gitHubRepoService.pushContentToOrigin("demoadd", gitRepoFolder.getRoot(),repoVO.get());

        //Clean up
        gitHubRepoService.deleteRepo("demoadd");
    }

}
