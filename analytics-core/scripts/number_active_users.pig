/*
 *
 * CODENVY CONFIDENTIAL
 * ________________
 *
 * [2012] - [2013] Codenvy, S.A.
 * All Rights Reserved.
 * NOTICE: All information contained herein is, and remains
 * the property of Codenvy S.A. and its suppliers,
 * if any. The intellectual and technical concepts contained
 * herein are proprietary to Codenvy S.A.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Codenvy S.A..
 */

IMPORT 'macros.pig';

f1 = loadResources('$log');
f2 = filterByEvent(f1, '$EVENT');
f = filterByDate(f2, '$FROM_DATE', '$TO_DATE');

a1 = extractUser(f);
a = FOREACH a1 GENERATE user;

b = LOAD '$LOAD_DIR' USING PigStorage() AS (user : chararray);

c1 = UNION a, b;
c = DISTINCT c1;

STORE c INTO '$STORE_DIR' USING PigStorage();

result = countAll(c);
