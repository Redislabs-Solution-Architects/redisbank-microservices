package com.redis.demos.redisbanktransactions;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.redis.demos.redisbanktransactions.Config.StompConfig;
import com.redis.lettucemod.api.StatefulRedisModulesConnection;
import com.redis.lettucemod.api.sync.RediSearchCommands;
import com.redis.lettucemod.search.SearchOptions;
import com.redis.lettucemod.search.SearchResults;

@RestController
public class TransactionsController {

    private static final String SEARCH_INDEX = "transactions_idx";

    private final Config config;
    private final StatefulRedisModulesConnection<String, String> amRedis;

    public TransactionsController(Config config, StatefulRedisModulesConnection<String, String> srmc) {
        this.config = config;
        this.amRedis = srmc;
    }

    @GetMapping("/config/stomp")
    public StompConfig stompConfig() {
        return config.getStomp();
    }

    @GetMapping("/transactions")
    public SearchResults<String, String> listTransactions(@RequestParam String iban) {
        RediSearchCommands<String, String> commands = amRedis.sync();
        String searchQuery = "'@toAccount:" + iban + "'";
        SearchResults<String, String> results = commands.ftSearch(SEARCH_INDEX, searchQuery);
        return results;
    }

    @GetMapping("/search")
    @SuppressWarnings("all")
    public SearchResults<String, String> searchTransactions(@RequestParam("term") String term,
            @RequestParam("iban") String iban) {

        RediSearchCommands<String, String> commands = amRedis.sync();

        SearchOptions options = SearchOptions
                .builder().highlight(SearchOptions.Highlight.builder().field("description").field("fromAccountName")
                        .field("transactionType").tags("<mark>", "</mark>").build())
                .build();

        String searchQuery = "'@toAccount:" + iban + "(@description:" + term + " | @fromAccountName:" + term
                + " | @transactionType:" + term + ")'";

        SearchResults<String, String> results = commands.ftSearch(SEARCH_INDEX, searchQuery, options);
        return results;
    }

}
