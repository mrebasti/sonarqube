// Generated by CoffeeScript 1.7.1
(function() {
  var __hasProp = {}.hasOwnProperty,
    __extends = function(child, parent) { for (var key in parent) { if (__hasProp.call(parent, key)) child[key] = parent[key]; } function ctor() { this.constructor = child; } ctor.prototype = parent.prototype; child.prototype = new ctor(); child.__super__ = parent.prototype; return child; };

  define(['backbone.marionette', 'handlebars', 'quality-gate/models/quality-gate'], function(Marionette, Handlebars, QualityGate) {
    var QualityGateDetailHeaderView;
    return QualityGateDetailHeaderView = (function(_super) {
      __extends(QualityGateDetailHeaderView, _super);

      function QualityGateDetailHeaderView() {
        return QualityGateDetailHeaderView.__super__.constructor.apply(this, arguments);
      }

      QualityGateDetailHeaderView.prototype.template = Handlebars.compile(jQuery('#quality-gate-detail-header-template').html());

      QualityGateDetailHeaderView.prototype.spinner = '<i class="spinner"></i>';

      QualityGateDetailHeaderView.prototype.modelEvents = {
        'change': 'render'
      };

      QualityGateDetailHeaderView.prototype.events = {
        'click #quality-gate-rename': 'renameQualityGate',
        'click #quality-gate-copy': 'copyQualityGate',
        'click #quality-gate-delete': 'deleteQualityGate',
        'click #quality-gate-set-as-default': 'setAsDefault',
        'click #quality-gate-unset-as-default': 'unsetAsDefault'
      };

      QualityGateDetailHeaderView.prototype.renameQualityGate = function() {
        this.options.app.qualityGateEditView.method = 'rename';
        this.options.app.qualityGateEditView.model = this.model;
        return this.options.app.qualityGateEditView.show();
      };

      QualityGateDetailHeaderView.prototype.copyQualityGate = function() {
        var copiedModel;
        copiedModel = new QualityGate(this.model.toJSON());
        copiedModel.set('default', false);
        this.options.app.qualityGateEditView.method = 'copy';
        this.options.app.qualityGateEditView.model = copiedModel;
        return this.options.app.qualityGateEditView.show();
      };

      QualityGateDetailHeaderView.prototype.deleteQualityGate = function() {
        var message;
        message = this.model.get('default') ? 'quality_gates.delete.confirm.default' : 'quality_gates.delete.confirm.message';
        if (confirm(t(message).replace('{0}', this.model.get('name')))) {
          this.showSpinner();
          return jQuery.ajax({
            type: 'POST',
            url: "" + baseUrl + "/api/qualitygates/destroy",
            data: {
              id: this.model.id
            }
          }).always((function(_this) {
            return function() {
              return _this.hideSpinner();
            };
          })(this)).done((function(_this) {
            return function() {
              return _this.options.app.deleteQualityGate(_this.model.id);
            };
          })(this));
        }
      };

      QualityGateDetailHeaderView.prototype.changeDefault = function(set) {
        var data, method;
        this.showSpinner();
        data = set ? {
          id: this.model.id
        } : {};
        method = set ? 'set_as_default' : 'unset_default';
        return jQuery.ajax({
          type: 'POST',
          url: "" + baseUrl + "/api/qualitygates/" + method,
          data: data
        }).always((function(_this) {
          return function() {
            return _this.hideSpinner();
          };
        })(this)).done((function(_this) {
          return function() {
            _this.options.app.unsetDefaults(_this.model.id);
            return _this.model.set('default', !_this.model.get('default'));
          };
        })(this));
      };

      QualityGateDetailHeaderView.prototype.setAsDefault = function() {
        return this.changeDefault(true);
      };

      QualityGateDetailHeaderView.prototype.unsetAsDefault = function() {
        return this.changeDefault(false);
      };

      QualityGateDetailHeaderView.prototype.showSpinner = function() {
        this.$el.hide();
        return jQuery(this.spinner).insertBefore(this.$el);
      };

      QualityGateDetailHeaderView.prototype.hideSpinner = function() {
        this.$el.prev().remove();
        return this.$el.show();
      };

      QualityGateDetailHeaderView.prototype.serializeData = function() {
        return _.extend(QualityGateDetailHeaderView.__super__.serializeData.apply(this, arguments), {
          canEdit: this.options.app.canEdit
        });
      };

      return QualityGateDetailHeaderView;

    })(Marionette.ItemView);
  });

}).call(this);