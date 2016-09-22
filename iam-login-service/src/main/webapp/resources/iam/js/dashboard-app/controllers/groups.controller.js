'use strict';

angular.module('dashboardApp').controller('GroupsController', GroupsController);

GroupsController.$inject = [ '$scope', '$rootScope', '$uibModal', '$state',
		'$filter', 'filterFilter', 'scimFactory', 'ModalService' ];

function GroupsController($scope, $rootScope, $uibModal, $state, $filter,
		filterFilter, scimFactory, ModalService) {

	var gc = this;

	// group data
	gc.groups = [];

	// filtered groups to show
	gc.filtered = [];
	// text to find to filter groups
	gc.searchText = "";

	// pagination controls
	gc.currentPage = 1;
	gc.entryLimit = 10; // items per page

	// functions
	gc.getAllGroups = getAllGroups;
	gc.getGroups = getGroups;
	gc.resetFilters = resetFilters;
	gc.rebuildFilteredList = rebuildFilteredList;

	// add group
	gc.clickToOpen = clickToOpen;

	// delete group
	gc.deleteGroup = deleteGroup;
	gc.removeGroupFromList = removeGroupFromList;

	gc.loadGroupList = loadGroupList;

	// Controller actions:
	gc.resetFilters()
	gc.loadGroupList(); // eval gc.groups


	function rebuildFilteredList() {
		
		gc.filtered = filterFilter(gc.groups, {'displayName': gc.searchText});
		gc.filtered = $filter('orderBy')(gc.filtered, "displayName", false);
	}

	function resetFilters() {
		gc.searchText = "";
	}

	$scope.$watch('gc.searchText', function() {

		gc.rebuildFilteredList();
	});

	function loadGroupList() {

		$rootScope.groupsLoadingProgress = 0;
		gc.loadingModal = $uibModal
		.open({
			animation: false,
			templateUrl : '/resources/iam/template/dashboard/groups/loading-modal.html'
		});

		gc.loadingModal.opened.then(function() {
			gc.getAllGroups();
		});
	}

	function getAllGroups() {

		gc.groups = [];
		gc.getGroups(1, gc.entryLimit);
	}

	function getGroups(startIndex, count) {

		scimFactory
				.getGroups(startIndex, count)
				.then(
						function(response) {
							angular.forEach(response.data.Resources, function(
									group) {
								gc.groups.push(group);
							});
							gc.rebuildFilteredList();
							if (response.data.totalResults > (response.data.startIndex - 1 + response.data.itemsPerPage)) {
								gc.getGroups(startIndex + count, count);
								$rootScope.groupsLoadingProgress = Math.floor((startIndex + count) * 100 / response.data.totalResults);
							} else {
								$rootScope.groupsLoadingProgress = 100;
								gc.loadingModal.dismiss("Cancel");
							}
						}, function(error) {
							console.log("getGroups error", error);
							gc.loadingModal.dismiss("Error");
							gc.textAlert = error.data.error_description || error.data.detail;
							gc.operationResult = 'err';
						});
	}

	function clickToOpen() {
		var modalInstance = $uibModal.open({
			templateUrl : '/resources/iam/template/dashboard/groups/newgroup.html',
			controller : 'AddGroupController',
			controllerAs: 'addGroupCtrl'
		});
		modalInstance.result.then(function(createdGroup) {
			console.info(createdGroup);
			gc.groups.push(createdGroup);
			gc.rebuildFilteredList();
			gc.textAlert = `Group ${createdGroup.displayName} added successfully`;
			gc.operationResult = 'ok';
		}, function() {
			console.info('Modal dismissed at: ', new Date());
		});
	}
	;

	function removeGroupFromList(group) {

		var i = gc.groups.indexOf(group);
		gc.groups.splice(i, 1);

		gc.rebuildFilteredList();
	}

	function deleteGroup(group) {
		
		var modalOptions = {
			closeButtonText: 'Cancel',
			actionButtonText: 'Delete Group',
			headerText: "Delete Group «" + group.displayName + "»",
			bodyText: `Are you sure you want to delete group «${group.displayName}»?`	
		};
		
		ModalService.showModal({}, modalOptions).then(
			function (){
				scimFactory.deleteGroup(group.id)
					.then(function(response) {
						gc.removeGroupFromList(group);
						$rootScope.loggedUser.totGroups = $rootScope.loggedUser.totGroups -1;
						gc.textAlert = `Group ${group.displayName} deleted successfully`;
						gc.operationResult = 'ok';
					}, function(error) {
						gc.textAlert = error.data.error_description || error.data.detail;
						gc.operationResult = 'err';
					});
			});
	}
}