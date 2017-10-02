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

import com.redhat.developers.config.CheBootalizrProperties;
import com.redhat.developers.service.GitHubRepoService;
import com.redhat.developers.vo.RepoVO;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, classes = GitHubRepoServiceTest.TestConfig.class)
@ActiveProfiles("ut")
public class GitHubRepoServiceTest {

    @Mock
    GitHubRepoService gitHubRepoService;

    @Test
    public void should_create_and_return_repo_demo() throws IOException {

        when(gitHubRepoService.createRepo("demo", "my demo repo"))
            .thenReturn(Optional.of(RepoVO.builder().name("demo").build()));

        Optional<RepoVO> optRepo = gitHubRepoService.createRepo("demo", "my demo repo");

        assertThat(optRepo.isPresent()).isTrue();

        assertThat(optRepo.get().getName()).isEqualTo("demo");
    }

    @Test
    public void should_not_return_repo() throws IOException {

        when(gitHubRepoService.getRepo("demo21"))
            .thenReturn(Optional.ofNullable(null));

        Optional<RepoVO> optRepo = gitHubRepoService.getRepo("demo21");

        assertThat(optRepo.isPresent()).isFalse();
    }

    @Test
    public void should_delete_repo() throws IOException {

        when(gitHubRepoService.deleteRepo("demodel"))
            .thenReturn(true);

        boolean isDeleted = gitHubRepoService.deleteRepo("demodel");

        assertThat(isDeleted).isTrue();
    }

    @Configuration
    public static class TestConfig {
        @Bean
        public CheBootalizrProperties cheBootalizrProperties() {
            return new CheBootalizrProperties();
        }
    }
}
