$(function () {

    var addTag = function (id, name) {
        if ($("#depsTags div[data-id='" + id + "']").length == 0) {
            $("#depsTags").append("<div class='tag' data-id='" + id + "'>" + name +
                "<button type='button' class='close' aria-label='Close'><span aria-hidden='true'>&times;</span></button></div>");
        }
    };

    var removeTag = function (id) {
        $("#depsTags div[data-id='" + id + "']").remove();
    };

    var isTagExists = function (id) {
        return $("#depsTags div[data-id='" + id + "']") === undefined;
    };

    var depsSearcher = function (engine, bootVersion) {

        $.getJSON({
            url: '/ui/dependencies.json?version=' + bootVersion
        }).done(function (data) {
            engine.clear();
            $.each(data.dependencies, function (i, item) {
                if (item.weight === undefined) {
                    item.weight = 0;
                }
            });
            engine.add(data.dependencies);
        })
    };

    var bDeps = new Bloodhound({
        datumTokenizer: Bloodhound.tokenizers.obj.nonword('group', 'name', 'description', 'keywords'),
        queryTokenizer: Bloodhound.tokenizers.nonword,
        identify: function (item) {
            return item.id;
        },
        sorter: function (left, right) {
            return left.weight - right.weight;
        },
        cache: false,
        limit: 5
    });

    depsSearcher(bDeps, $('#bootVersion').val());

    $("#bootVersion").on("change", function (e) {
        depsSearcher(bDeps, this.value);
    });

    $('#depsList').typeahead({
        minLength: 2,
        autoSelect: true,
    }, {
        name: 'bDeps',
        display: 'name',
        source: bDeps,
        templates: {
            suggestion: function (dep) {
                return "<div><strong>" + dep.name + "</strong>&nbsp;&nbsp;<small>" + dep.description + "</small></div>"
            }
        }
    });


    $('#depsList').bind('typeahead:select', function (event, dep) {
        if (!isTagExists(dep.id)) {
            addTag(dep.id, dep.name);
        }
    });

    $('#depsTags').on('click', "button", function () {
        var id = $(this).parent().attr('data-id');
        removeTag(id);
    })

});
