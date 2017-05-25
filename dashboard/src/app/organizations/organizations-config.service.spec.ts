/*
 *  [2015] - [2017] Codenvy, S.A.
 *  All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Codenvy S.A. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Codenvy S.A.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Codenvy S.A..
 */
'use strict';
import {CodenvyAPIBuilder} from '../../components/api/builder/codenvy-api-builder.factory';
import {CodenvyHttpBackend} from '../../components/api/test/codenvy-http-backend';
import {OrganizationsConfigServiceMock} from './organizations-config.service.mock';

describe('OrganizationsConfig >', () => {

  let $route;

  let $injector: ng.auto.IInjectorService;

  let $rootScope;

  let $location;

  let $httpBackend;

  let mock;

  /**
   * Setup module
   */
  beforeEach(angular.mock.module('codenvyDashboard'));

  /**
   * Inject service and http backend
   */
  beforeEach(inject((_$injector_: ng.auto.IInjectorService,
                     _$location_: ng.ILocationService,
                     _$route_: ng.route.IRouteService,
                     _$rootScope_: ng.IRootScopeService,
                     _codenvyAPIBuilder_: CodenvyAPIBuilder,
                     _codenvyHttpBackend_: CodenvyHttpBackend,
                     _cheAPIBuilder_: any, _cheHttpBackend_: any) => {
    $injector = _$injector_;
    $location = _$location_;
    $route = _$route_;
    $rootScope = _$rootScope_;
    $httpBackend = _codenvyHttpBackend_.getHttpBackend();

    mock = new OrganizationsConfigServiceMock(_codenvyAPIBuilder_, _codenvyHttpBackend_, _cheAPIBuilder_, _cheHttpBackend_);

    mock.mockData();

    $httpBackend.when('GET', 'app/dashboard/main-dashboard.html').respond('<main-dashboard></main-dashboard>');
    $httpBackend.when('GET', 'app/organizations/create-organizations/create-organizations.html').respond('<create-organization-template></create-organization-template>');

    $httpBackend.flush();
  }));

  /**
   * Check assertion after the test
   */
  afterEach(() => {
    $httpBackend.verifyNoOutstandingRequest();
    $httpBackend.verifyNoOutstandingExpectation();
  });

  describe('create root organization "/admin/create-organization" >', () => {

    it('should resolve route and return data', () => {

      const route = $route.routes['/admin/create-organization'];
      const resolveBlock = route.resolve.initData;

      // stub functions
      const callbacks = {
        testResolve: () => {
        },
        testReject: () => {
        }
      };

      // create spies
      spyOn(callbacks, 'testResolve');
      spyOn(callbacks, 'testReject');

      const service = $injector.invoke(resolveBlock);

      service
        .then(callbacks.testResolve)
        .catch(callbacks.testReject);

      $httpBackend.flush();

      expect(callbacks.testResolve).toHaveBeenCalledWith({
        parentQualifiedName: '',
        parentOrganizationId: '',
        parentOrganizationMembers: []
      });
      expect(callbacks.testReject).not.toHaveBeenCalled();
    });

  });

  describe('create sub-organization "/admin/create-organization/{parentId}" >', () => {

    it('should resolve route and return data', () => {
      const organizations = mock.getOrganizations();
      const parentOrg = organizations[0];
      const users = mock.getUsersByOrganizationId(parentOrg.id);
      const buildIdsList = (res: string[], user: che.IUser) => {
        res.push(user.id);
        return res;
      };

      $route.current.params.parentQualifiedName = parentOrg.qualifiedName;
      const route = $route.routes['/admin/create-organization/:parentQualifiedName*'];
      const resolveBlock = route.resolve.initData;

      // stub functions
      const callbacks = {
        testResolve: (data: any) => {
          expect(data.parentQualifiedName).toEqual(parentOrg.qualifiedName);
          expect(data.parentOrganizationId).toEqual(parentOrg.id);
          expect(data.parentOrganizationMembers.length).toEqual(users.length);

          const parentMemberIds = data.parentOrganizationMembers.reduce(buildIdsList, []).sort();
          const userIds = users.reduce(buildIdsList, []).sort();
          expect(parentMemberIds).toEqual(userIds);
        },
        testReject: () => { }
      };

      // create spies
      spyOn(callbacks, 'testResolve').and.callThrough();
      spyOn(callbacks, 'testReject');

      const service = $injector.invoke(resolveBlock);

      service
        .then(callbacks.testResolve)
        .catch(callbacks.testReject);

      $httpBackend.flush();

      expect(callbacks.testResolve).toHaveBeenCalled();
      expect(callbacks.testReject).not.toHaveBeenCalled();
    });

    it('should resolve route and return data if organizations request fails', () => {
      const organizations = mock.getOrganizations();
      const parentOrg = organizations[0];

      $route.current.params.parentQualifiedName = parentOrg.qualifiedName;
      const route = $route.routes['/admin/create-organization/:parentQualifiedName*'];
      const resolveBlock = route.resolve.initData;

      // stub functions
      const callbacks = {
        testResolve: () => { },
        testReject: () => { }
      };

      // create spies
      spyOn(callbacks, 'testResolve');
      spyOn(callbacks, 'testReject');

      // make response for organizations list fail
      $httpBackend.expect('GET', /\/api\/organization(\?.*$)?/).respond(500, [], {message: 'response failed'});

      const service = $injector.invoke(resolveBlock);

      service
        .then(callbacks.testResolve)
        .catch(callbacks.testReject);

      $httpBackend.flush();

      expect(callbacks.testResolve).toHaveBeenCalledWith({
        parentQualifiedName: parentOrg.qualifiedName,
        parentOrganizationId: '',
        parentOrganizationMembers: []
      });
      expect(callbacks.testReject).not.toHaveBeenCalled();
    });

    it('should resolve route and return data if parent organization is not found', () => {
      const fakeQualifiedName = 'fake/qualified/name';

      $httpBackend.when('GET', 'app/organizations/create-organizations/create-organizations.html').respond('<create-organization-template></create-organization-template>');

      $route.current.params.parentQualifiedName = fakeQualifiedName;
      const route = $route.routes['/admin/create-organization/:parentQualifiedName*'];
      const resolveBlock = route.resolve.initData;

      // stub functions
      const callbacks = {
        testResolve: () => { },
        testReject: () => { }
      };

      // create spies
      spyOn(callbacks, 'testResolve');
      spyOn(callbacks, 'testReject');

      const service = $injector.invoke(resolveBlock);

      service
        .then(callbacks.testResolve)
        .catch(callbacks.testReject);

      $httpBackend.flush();

      expect(callbacks.testResolve).toHaveBeenCalledWith({
        parentQualifiedName: fakeQualifiedName,
        parentOrganizationId: '',
        parentOrganizationMembers: []
      });
      expect(callbacks.testReject).not.toHaveBeenCalled();
    });

  });

  describe('get root organization details "/organization/{rootOrganization}" >', () => {

    it('should resolve route and return data', () => {
      const organizations = mock.getOrganizations();
      const parentOrg = organizations[0];

      $route.current.params.organizationName = parentOrg.qualifiedName;
      const route = $route.routes['/organization/:organizationName*'];
      const resolveBlock = route.resolve.initData;

      // stub functions
      const callbacks = {
        testResolve: (initData: any) => {
          // this is necessary because of different types
          const equal = angular.equals(initData.organization, parentOrg);
          expect(equal).toBeTruthy();

          expect(initData.parentOrganizationMembers).toEqual([]);
        },
        testReject: () => { }
      };

      // create spies
      spyOn(callbacks, 'testResolve').and.callThrough();
      spyOn(callbacks, 'testReject');

      const service = $injector.invoke(resolveBlock);

      service
        .then(callbacks.testResolve)
        .catch(callbacks.testReject);

      $httpBackend.flush();

      expect(callbacks.testResolve).toHaveBeenCalled();
      expect(callbacks.testReject).not.toHaveBeenCalled();
    });

    it('should resolve route and return data if request for organizations list fails', () => {
      const organizations = mock.getOrganizations();
      const parentOrg = organizations[0];

      $route.current.params.organizationName = parentOrg.qualifiedName;
      const route = $route.routes['/organization/:organizationName*'];
      const resolveBlock = route.resolve.initData;

      // stub functions
      const callbacks = {
        testResolve: (initData: any) => {
          expect(initData.organization).toBeFalsy();
          expect(initData.parentOrganizationMembers).toEqual([]);
        },
        testReject: () => { }
      };

      // create spies
      spyOn(callbacks, 'testResolve').and.callThrough();
      spyOn(callbacks, 'testReject');

      // make response for organizations list fail
      $httpBackend.expect('GET', /\/api\/organization(\?.*$)?/).respond(500, [], {message: 'response failed'});

      const service = $injector.invoke(resolveBlock);

      service
        .then(callbacks.testResolve)
        .catch(callbacks.testReject);

      $httpBackend.flush();

      expect(callbacks.testResolve).toHaveBeenCalled();
      expect(callbacks.testReject).not.toHaveBeenCalled();
    });

  });

  describe('get sub-organization details "/organization/{rootOrganization}/{sub-organization}" >', () => {

    it('should resolve route and return data', () => {
      const organizations = mock.getOrganizations();
      // get sub-organization
      const subOrg = organizations[1];

      // get parent organization users
      const users = mock.getUsersByOrganizationId(subOrg.parent);
      const buildIdsList = (res: string[], user: che.IUser) => {
        res.push(user.id);
        return res;
      };

      $route.current.params.organizationName = subOrg.qualifiedName;
      const route = $route.routes['/organization/:organizationName*'];
      const resolveBlock = route.resolve.initData;

      // stub functions
      const callbacks = {
        testResolve: (initData: any) => {
          // this is necessary because of different types
          const organizationsAreEqual = angular.equals(initData.organization, subOrg);
          expect(organizationsAreEqual).toBeTruthy();

          expect(initData.parentOrganizationMembers.length).toEqual(users.length);

          const parentMemberIds = initData.parentOrganizationMembers.reduce(buildIdsList, []).sort();
          const userIds = users.reduce(buildIdsList, []).sort();
          expect(parentMemberIds).toEqual(userIds);
        },
        testReject: () => { }
      };

      // create spies
      spyOn(callbacks, 'testResolve').and.callThrough();
      spyOn(callbacks, 'testReject');

      const service = $injector.invoke(resolveBlock);

      service
        .then(callbacks.testResolve)
        .catch(callbacks.testReject);

      $httpBackend.flush();

      expect(callbacks.testResolve).toHaveBeenCalled();
      expect(callbacks.testReject).not.toHaveBeenCalled();
    });

    it('should resolve route and return data if request for organizations list fails', () => {
      const organizations = mock.getOrganizations();
      const subOrg = organizations[1];

      $route.current.params.organizationName = subOrg.qualifiedName;
      const route = $route.routes['/organization/:organizationName*'];
      const resolveBlock = route.resolve.initData;

      // stub functions
      const callbacks = {
        testResolve: (initData: any) => {
          expect(initData.organization).toBeFalsy();
          expect(initData.parentOrganizationMembers).toEqual([]);
        },
        testReject: () => { }
      };

      // create spies
      spyOn(callbacks, 'testResolve').and.callThrough();
      spyOn(callbacks, 'testReject');

      // make response for organizations list fail
      $httpBackend.expect('GET', /\/api\/organization(\?.*$)?/).respond(500, [], {message: 'response failed'});

      const service = $injector.invoke(resolveBlock);

      service
        .then(callbacks.testResolve)
        .catch(callbacks.testReject);

      $httpBackend.flush();

      expect(callbacks.testResolve).toHaveBeenCalled();
      expect(callbacks.testReject).not.toHaveBeenCalled();
    });

  });

});