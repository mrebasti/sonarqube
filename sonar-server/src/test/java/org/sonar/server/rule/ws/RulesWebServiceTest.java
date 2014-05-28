/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.rule.ws;

import com.google.common.collect.ImmutableSet;
import org.junit.*;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.server.debt.DebtRemediationFunction;
import org.sonar.api.server.ws.WebService;
import org.sonar.check.Cardinality;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.qualityprofile.db.ActiveRuleDto;
import org.sonar.core.qualityprofile.db.ActiveRuleParamDto;
import org.sonar.core.qualityprofile.db.QualityProfileDao;
import org.sonar.core.qualityprofile.db.QualityProfileDto;
import org.sonar.core.rule.RuleDto;
import org.sonar.core.rule.RuleParamDto;
import org.sonar.server.qualityprofile.persistence.ActiveRuleDao;
import org.sonar.server.rule.db.RuleDao;
import org.sonar.server.rule.index.RuleNormalizer;
import org.sonar.server.search.ws.SearchOptions;
import org.sonar.server.tester.ServerTester;
import org.sonar.server.user.MockUserSession;
import org.sonar.server.ws.WsTester;

import static org.fest.assertions.Assertions.assertThat;

public class RulesWebServiceTest {

  @ClassRule
  public static ServerTester tester = new ServerTester();

  private static final String API_ENDPOINT = "api/rules";
  private static final String API_SEARCH_METHOD = "search";
  private static final String API_SHOW_METHOD = "show";
  private static final String API_TAGS_METHOD = "tags";

  private RulesWebService ws;
  private RuleDao ruleDao;
  private DbSession session;

  WsTester wsTester;


  @Before
  public void setUp() throws Exception {
    tester.clearDbAndEs();
    ruleDao = tester.get(RuleDao.class);
    ws = tester.get(RulesWebService.class);
    wsTester = new WsTester(ws);
    session = tester.get(MyBatis.class).openSession(false);
  }

  @After
  public void after() {
    session.close();
  }

  @Test
  public void define() throws Exception {

    WebService.Context context = new WebService.Context();
    ws.define(context);

    WebService.Controller controller = context.controller(API_ENDPOINT);

    assertThat(controller).isNotNull();
    assertThat(controller.actions()).hasSize(6);
    assertThat(controller.action(API_SEARCH_METHOD)).isNotNull();
    assertThat(controller.action(API_SHOW_METHOD)).isNotNull();
    assertThat(controller.action(API_TAGS_METHOD)).isNotNull();
    assertThat(controller.action("set_tags")).isNotNull();
    assertThat(controller.action("set_note")).isNotNull();
    assertThat(controller.action("app")).isNotNull();
  }

  @Test
  public void show_rule() throws Exception {
    QualityProfileDto profile = newQualityProfile();
    tester.get(QualityProfileDao.class).insert(session, profile);

    RuleDto rule = newRuleDto(RuleKey.of(profile.getLanguage(), "S001"));
    ruleDao.insert(session, rule);

    ActiveRuleDto activeRuleDto = ActiveRuleDto.createFor(profile, rule)
      .setSeverity("BLOCKER");
    tester.get(ActiveRuleDao.class).insert(session, activeRuleDto);

    session.commit();

    MockUserSession.set();

    // 1. With Activation
    WsTester.TestRequest request = wsTester.newGetRequest(API_ENDPOINT, API_SHOW_METHOD);
    request.setParam(ShowAction.PARAM_KEY, rule.getKey().toString());
    request.setParam(ShowAction.PARAM_ACTIVES, "true");
    WsTester.Result result = request.execute();
    result.assertJson(this.getClass(), "show_rule_active.json", false);

    // 1. Default Activation (defaults to false)
    request = wsTester.newGetRequest(API_ENDPOINT, API_SHOW_METHOD);
    request.setParam(ShowAction.PARAM_KEY, rule.getKey().toString());
    result = request.execute();
    result.assertJson(this.getClass(), "show_rule_no_active.json", false);
  }


  @Test
  public void search_no_rules() throws Exception {

    MockUserSession.set();
    WsTester.TestRequest request = wsTester.newGetRequest(API_ENDPOINT, API_SEARCH_METHOD);

    WsTester.Result result = request.execute();

    result.assertJson(this.getClass(), "search_no_rules.json");
  }

  @Test
  public void search_2_rules() throws Exception {
    ruleDao.insert(session, newRuleDto(RuleKey.of("javascript", "S001")));
    ruleDao.insert(session, newRuleDto(RuleKey.of("javascript", "S002")));
    session.commit();

    MockUserSession.set();
    WsTester.TestRequest request = wsTester.newGetRequest(API_ENDPOINT, API_SEARCH_METHOD);
    WsTester.Result result = request.execute();

    result.assertJson(getClass(), "search_2_rules.json", false);
  }


  @Test
  public void search_debt_rules() throws Exception {
    ruleDao.insert(session, newRuleDto(RuleKey.of("javascript", "S001"))
      .setDefaultRemediationCoefficient("DefaultCoef")
      .setDefaultRemediationFunction("DefaultFunction")
      .setDefaultRemediationCoefficient("DefaultCoef")
      .setDefaultSubCharacteristicId(1));
    session.commit();


    MockUserSession.set();
    WsTester.TestRequest request = wsTester.newGetRequest(API_ENDPOINT, API_SEARCH_METHOD);
    request.setParam(SearchOptions.PARAM_FIELDS, "debtRemFn,debtChar");
    WsTester.Result result = request.execute();
    result.assertJson(this.getClass(), "search_debt_rule.json");
  }


  @Test
  public void search_all_active_rules() throws Exception {
    QualityProfileDto profile = newQualityProfile();
    tester.get(QualityProfileDao.class).insert(session, profile);

    RuleDto rule = newRuleDto(RuleKey.of(profile.getLanguage(), "S001"));
    ruleDao.insert(session, rule);

    ActiveRuleDto activeRule = newActiveRule(profile, rule);
    tester.get(ActiveRuleDao.class).insert(session, activeRule);

    session.commit();


    MockUserSession.set();
    WsTester.TestRequest request = wsTester.newGetRequest(API_ENDPOINT, API_SEARCH_METHOD);
    request.setParam(SearchOptions.PARAM_TEXT_QUERY, "S001");
    request.setParam(SearchAction.PARAM_ACTIVATION, "true");
    request.setParam(SearchOptions.PARAM_FIELDS, "");
    WsTester.Result result = request.execute();

    result.assertJson(this.getClass(), "search_active_rules.json");
  }


  @Test
  public void search_profile_active_rules() throws Exception {
    QualityProfileDto profile = newQualityProfile().setName("p1");
    tester.get(QualityProfileDao.class).insert(session, profile);

    QualityProfileDto profile2 = newQualityProfile().setName("p2");
    tester.get(QualityProfileDao.class).insert(session, profile2);

    session.commit();

    RuleDto rule = newRuleDto(RuleKey.of(profile.getLanguage(), "S001"));
    ruleDao.insert(session, rule);

    ActiveRuleDto activeRule = newActiveRule(profile, rule);
    tester.get(ActiveRuleDao.class).insert(session, activeRule);
    ActiveRuleDto activeRule2 = newActiveRule(profile2, rule);
    tester.get(ActiveRuleDao.class).insert(session, activeRule2);

    session.commit();


    MockUserSession.set();
    WsTester.TestRequest request = wsTester.newGetRequest(API_ENDPOINT, API_SEARCH_METHOD);
    request.setParam(SearchOptions.PARAM_TEXT_QUERY, "S001");
    request.setParam(SearchAction.PARAM_ACTIVATION, "true");
    request.setParam(SearchAction.PARAM_QPROFILE, profile2.getKey().toString());
    request.setParam(SearchOptions.PARAM_FIELDS, "");
    WsTester.Result result = request.execute();
    result.assertJson(this.getClass(), "search_profile_active_rules.json");
  }

  @Test
  public void search_all_active_rules_params() throws Exception {
    QualityProfileDto profile = newQualityProfile();
    tester.get(QualityProfileDao.class).insert(session, profile);

    RuleDto rule = newRuleDto(RuleKey.of(profile.getLanguage(), "S001"));
    ruleDao.insert(session, rule);

    session.commit();

    RuleParamDto param = RuleParamDto.createFor(rule)
      .setDefaultValue("some value")
      .setType("string")
      .setDescription("My small description")
      .setName("my_var");
    ruleDao.addRuleParam(rule, param, session);

    RuleParamDto param2 = RuleParamDto.createFor(rule)
      .setDefaultValue("other value")
      .setType("integer")
      .setDescription("My small description")
      .setName("the_var");
    ruleDao.addRuleParam(rule, param2, session);

    ActiveRuleDto activeRule = newActiveRule(profile, rule);
    tester.get(ActiveRuleDao.class).insert(session, activeRule);

    ActiveRuleParamDto activeRuleParam = ActiveRuleParamDto.createFor(param)
      .setValue("The VALUE");
    tester.get(ActiveRuleDao.class).addParam(activeRule, activeRuleParam, session);

    ActiveRuleParamDto activeRuleParam2 = ActiveRuleParamDto.createFor(param2)
      .setValue("The Other Value");
    tester.get(ActiveRuleDao.class).addParam(activeRule, activeRuleParam2, session);
    session.commit();


    MockUserSession.set();
    WsTester.TestRequest request = wsTester.newGetRequest(API_ENDPOINT, API_SEARCH_METHOD);
    request.setParam(SearchOptions.PARAM_TEXT_QUERY, "S001");
    request.setParam(SearchAction.PARAM_ACTIVATION, "true");
    request.setParam(SearchOptions.PARAM_FIELDS, "params");
    WsTester.Result result = request.execute();

    result.assertJson(this.getClass(), "search_active_rules_params.json", false);
  }


  @Test
  public void get_tags() throws Exception {
    QualityProfileDto profile = newQualityProfile();
    tester.get(QualityProfileDao.class).insert(session, profile);

    RuleDto rule = newRuleDto(RuleKey.of(profile.getLanguage(), "S001"))
      .setTags(ImmutableSet.of("hello", "world"));
    ruleDao.insert(session, rule);

    RuleDto rule2 = newRuleDto(RuleKey.of(profile.getLanguage(), "S002"))
      .setTags(ImmutableSet.of("java"))
      .setSystemTags(ImmutableSet.of("sys1"));
    ruleDao.insert(session, rule2);

    session.commit();

    MockUserSession.set();
    WsTester.TestRequest request = wsTester.newGetRequest(API_ENDPOINT, API_TAGS_METHOD);
    WsTester.Result result = request.execute();

    result.assertJson(this.getClass(), "get_tags.json", false);
  }

  @Test
  public void get_note_as_markdown_and_html() throws Exception {
    QualityProfileDto profile = newQualityProfile();
    tester.get(QualityProfileDao.class).insert(session, profile);

    RuleDto rule = newRuleDto(RuleKey.of(profile.getLanguage(), "S001"))
      .setNoteData("this is *bold*");
    ruleDao.insert(session, rule);

    session.commit();


    MockUserSession.set();
    WsTester.TestRequest request = wsTester.newGetRequest(API_ENDPOINT, API_SEARCH_METHOD);
    request.setParam(SearchOptions.PARAM_FIELDS, "htmlNote, mdNote");
    WsTester.Result result = request.execute();
    result.assertJson(this.getClass(), "get_note_as_markdown_and_html.json");
  }

  @Test
  public void filter_by_tags() throws Exception {
    ruleDao.insert(session, newRuleDto(RuleKey.of("java", "S001"))
      .setSystemTags(ImmutableSet.of("tag1")));
    ruleDao.insert(session, newRuleDto(RuleKey.of("java", "S002"))
      .setSystemTags(ImmutableSet.of("tag2")));

    session.commit();


    MockUserSession.set();
    WsTester.TestRequest request = wsTester.newGetRequest(API_ENDPOINT, API_SEARCH_METHOD);
    request.setParam(SearchAction.PARAM_TAGS, "tag1");
    request.setParam(SearchOptions.PARAM_FIELDS, "sysTags, tags");
    WsTester.Result result = request.execute();
    result.assertJson(this.getClass(), "filter_by_tags.json");
  }

  @Test
  @Ignore //TODO FIx Sort
  public void sort_by_name() throws Exception {
    ruleDao.insert(session, newRuleDto(RuleKey.of("java", "S002")).setName("Dodgy - Consider returning a zero length array rather than null "));
    ruleDao.insert(session, newRuleDto(RuleKey.of("java", "S001")).setName("Bad practice - Creates an empty zip file entry"));
    ruleDao.insert(session, newRuleDto(RuleKey.of("java", "S003")).setName("XPath rule"));
    session.commit();


    // 1. Sort Name Desc
    MockUserSession.set();
    WsTester.TestRequest request = wsTester.newGetRequest(API_ENDPOINT, API_SEARCH_METHOD);
    request.setParam(SearchOptions.PARAM_FIELDS, "");
    request.setParam(SearchOptions.PARAM_SORT, RuleNormalizer.RuleField.NAME.field());
    request.setParam(SearchOptions.PARAM_ASCENDING, Boolean.TRUE.toString());

    WsTester.Result result = request.execute();
    result.assertJson("{\"total\":3,\"p\":1,\"ps\":10,\"rules\":[{\"key\":\"java:S001\"},{\"key\":\"java:S002\"},{\"key\":\"java:S003\"}]}");

    // 2. Sort Name ASC
    request = wsTester.newGetRequest(API_ENDPOINT, API_SEARCH_METHOD);
    request.setParam(SearchOptions.PARAM_FIELDS, "");
    request.setParam(SearchOptions.PARAM_SORT, RuleNormalizer.RuleField.NAME.field());
    request.setParam(SearchOptions.PARAM_ASCENDING, Boolean.FALSE.toString());

    result = request.execute();
    result.assertJson("{\"total\":3,\"p\":1,\"ps\":10,\"rules\":[{\"key\":\"java:S003\"},{\"key\":\"java:S002\"},{\"key\":\"java:S001\"}]}");

  }


  private QualityProfileDto newQualityProfile() {
    return new QualityProfileDto()
      .setLanguage("java")
      .setName("My Profile");
  }

  private RuleDto newRuleDto(RuleKey ruleKey) {
    return new RuleDto()
      .setRuleKey(ruleKey.rule())
      .setRepositoryKey(ruleKey.repository())
      .setName("Rule " + ruleKey.rule())
      .setDescription("Description " + ruleKey.rule())
      .setStatus(RuleStatus.READY.toString())
      .setConfigKey("InternalKey" + ruleKey.rule())
      .setSeverity(Severity.INFO)
      .setCardinality(Cardinality.SINGLE)
      .setLanguage("js")
      .setRemediationFunction(DebtRemediationFunction.Type.LINEAR.toString())
      .setDefaultRemediationFunction(DebtRemediationFunction.Type.LINEAR_OFFSET.toString())
      .setRemediationCoefficient("1h")
      .setDefaultRemediationCoefficient("5d")
      .setRemediationOffset("5min")
      .setDefaultRemediationOffset("10h")
      .setEffortToFixDescription(ruleKey.repository() + "." + ruleKey.rule() + ".effortToFix");
  }

  private ActiveRuleDto newActiveRule(QualityProfileDto profile, RuleDto rule) {
    return ActiveRuleDto.createFor(profile, rule)
      .setInheritance("none")
      .setSeverity("BLOCKER");
  }
}