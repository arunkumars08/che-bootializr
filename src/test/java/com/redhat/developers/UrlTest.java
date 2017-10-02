package com.redhat.developers;

import com.redhat.developers.utils.GeneralUtil;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class UrlTest {

    @Test
    public void should_return_url_without_dotgit() {
        String url = "https://github.com/skshiftio/demoadd2.git";
        url = GeneralUtil.sanitizeGitUrl(url);
        assertThat(url).isEqualTo("https://github.com/skshiftio/demoadd2");

    }
}
