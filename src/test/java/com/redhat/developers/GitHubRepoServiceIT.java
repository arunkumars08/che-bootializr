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
import com.redhat.developers.service.GitHubRepoServiceImpl;
import com.redhat.developers.service.RepoService;
import com.redhat.developers.vo.RepoVO;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, classes = CheBootializrConfiguration.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class GitHubRepoServiceIT {

    @Autowired
    RepoService repoService;

    @Test
    public void should_create_a_repo_called_demo() throws Exception {
        assertThat(repoService).isNotNull();
        repoService.createRepo("demo", "my demo repo");
    }

    @Test
    public void should_delete_a_repo_called_demo() throws Exception {
        assertThat(repoService).isNotNull();
        boolean isDeleted = repoService.deleteRepo("demo");
        assertThat(isDeleted).isTrue();
    }

    @Test
    public void will_not_delete_repo() throws Exception {
        assertThat(repoService).isNotNull();
        boolean isDeleted = repoService.deleteRepo("demo2");
        assertThat(isDeleted).isFalse();
    }

    @Test
    public void should_get_repo_getdemo() throws Exception {
        assertThat(repoService).isNotNull();
        Optional<RepoVO> createdRepo = repoService.createRepo("getdemo", "my get demo repo");
        assertThat(createdRepo.isPresent()).isTrue();

        await().atMost(10, TimeUnit.SECONDS)
            .until(() -> {
                //Awaitality
                Optional<RepoVO> getRepo = repoService.getRepo("getdemo");
                assertThat(getRepo.isPresent()).isTrue();

                assertThat(createdRepo.get().getName()).isEqualTo(getRepo.get().getName());
                assertThat(createdRepo.get().getFqdn()).isEqualTo(getRepo.get().getFqdn());

                //Cleanup
                return repoService.deleteRepo("getdemo");
            });
    }

}
