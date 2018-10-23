package org.openlca.jsonld.input;

import java.util.Objects;

import org.openlca.core.database.CategoryDao;
import org.openlca.core.model.Category;
import org.openlca.core.model.ModelType;
import org.openlca.jsonld.Json;
import org.openlca.util.Categories;

import com.google.gson.JsonObject;

class CategoryImport extends BaseImport<Category> {

	private CategoryImport(String refId, ImportConfig conf) {
		super(ModelType.CATEGORY, refId, conf);
	}

	static Category run(String refId, ImportConfig conf) {
		return new CategoryImport(refId, conf).run();
	}

	@Override
	Category map(JsonObject json, Category model) {
		if (json == null)
			return null;
		boolean isNew = false;
		String refId = Json.getString(json, "@id");
		if (model == null) {
			model = new Category();
			isNew = true;
		}
		In.mapAtts(json, model, model.getId(), conf);
		model.setModelType(Json.getEnum(json, "modelType", ModelType.class));
		if (!isNew || model.getCategory() == null)
			model = conf.db.put(model);
		else
			model = updateParent(model);
		if (!refId.equals(model.getRefId()))
			conf.db.categoryRefIdMapping.put(refId, model.getRefId());
		return model;
	}

	@Override
	protected Category get(String refId) {
		if (conf.db.categoryRefIdMapping.containsKey(refId))
			refId = conf.db.categoryRefIdMapping.get(refId);
		return conf.db.get(ModelType.CATEGORY, refId);
	}

	@Override
	Category map(JsonObject json, long id) {
		return map(json, new CategoryDao(conf.db.getDatabase()).getForId(id));
	}

	private Category updateParent(Category category) {
		// CategoryDao.update/insert will reassign a new ref id,
		// it won't be possible to make the match with the updated child
		// category, so we need to know which id will be generated by the db
		String refId = Categories.createRefId(category);
		Category parent = category.getCategory();
		// now check if category with id (generated) already exists
		for (Category child : parent.getChildCategories()) {
			if (Objects.equals(child.getRefId(), refId))
				return child;
		}
		parent.getChildCategories().add(category);
		parent = conf.db.updateChilds(parent);
		for (Category child : parent.getChildCategories()) {
			if (Objects.equals(child.getRefId(), refId))
				return child;
		}
		return null;
	}

}
