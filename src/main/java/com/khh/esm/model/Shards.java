package com.khh.esm.model;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class Shards {
    Long total;
    Long successful;
    Long skipped;
    Long failed;
}

